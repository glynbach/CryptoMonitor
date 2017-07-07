package com.kieral.cryptomon.service.arb.execution;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.LiquidityEntry;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.PollListener;
import com.kieral.cryptomon.service.PollingService;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.exception.NotEnoughFundsException;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstructionFactory;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.service.util.TradingUtils;
import com.kieral.cryptomon.service.util.Tuple2;

public class ExecutionMonitor implements PollListener {

	// Todo; pull these from a config
	private static BigDecimal MAXIMUM_DISCREPANCY = new BigDecimal("0.1");
	private static BigDecimal INCREMENT = new BigDecimal("0.01");
	private static BigDecimal MINIMUM_AMOUNT = new BigDecimal("0.0001");
	private static BigDecimal MINIMUM_PRICE = new BigDecimal("0.00000001");
	private static long MINIMUM_POLLING_INTERVAL = 500;
	
	private final ExecutorService executionMonitorDaemon = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ExecutionMonitorDaemon");
			thread.setDaemon(true);
			return thread;
		}});
	private final static AtomicInteger counter = new AtomicInteger(); 
	private final ExecutorService executionMonitorPriorityDaemon = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ExecutionMonitorPriorityDaemon-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}});
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private CountDownLatch pollingLatch = new CountDownLatch(1);

	private final OrderService orderService;	
	private final BalanceService balanceService;
	private final ArbService arbService;	
	private final BackOfficeService backOfficeService;
	private final ArbInstruction arbInstruction;
	private final PollingService pollingService;
	private final List<Order> longOrders = new LinkedList<Order>();
	private final List<Order> shortOrders = new LinkedList<Order>();
	private final ReentrantLock statusLock = new ReentrantLock();
	
	public ExecutionMonitor(OrderService orderService, BalanceService balanceService, ArbService arbService, 
			BackOfficeService backOfficeService, ArbInstruction arbInstruction, PollingService pollingService,
			long pollingIntervalMillis) throws NotEnoughFundsException {
		if (orderService == null)
			throw new IllegalArgumentException("orderService can not be null");
		if (balanceService == null)
			throw new IllegalArgumentException("balanceService can not be null");
		if (arbService == null)
			throw new IllegalArgumentException("arbService can not be null");
		if (backOfficeService == null)
			throw new IllegalArgumentException("backOfficeService can not be null");
		if (pollingService == null)
			throw new IllegalArgumentException("pollingService can not be null");
		if (pollingIntervalMillis < MINIMUM_POLLING_INTERVAL)
			pollingIntervalMillis = MINIMUM_POLLING_INTERVAL;
		this.orderService = orderService;
		this.balanceService = balanceService;
		this.arbService = arbService;
		this.backOfficeService = backOfficeService;
		this.arbInstruction = arbInstruction;
		this.pollingService = pollingService;
		if (arbService.isSuspended())
			throw new IllegalStateException("Not able to execute when arbService is suspended");
		validateArbInstruction(arbInstruction);
		reserveBalances(arbInstruction);
		logger.info("Executing arb instruction {}", arbInstruction);
		Tuple2<OrderStatus, OrderStatus> orderStatuses = placeOrders(arbInstruction.getLeg(Side.BID), arbInstruction.getLeg(Side.ASK));
		if (orderStatuses.getA() != OrderStatus.OPEN || orderStatuses.getB() != OrderStatus.OPEN) {
			logger.warn("Not all the orders have been placed; long status={} short stataus={} - run an immediate review",
					orderStatuses.getA(), orderStatuses.getB());
			queryStatus();
			reviewStatus();
		}
		executionMonitorDaemon.submit(new StatusCheckTask());
		pollingService.changePollingInterval(pollingIntervalMillis);
		pollingService.registerListener("ExecutionMonitor", this);
	}
	
	public boolean isClosed() {
		return isClosed(getStatus());
	}

	private boolean isClosed(OrderStatusSummary status) {
		return status != null && OrderStatus.CLOSED_ORDER.contains(status.getLongLegStatus()) && 
				OrderStatus.CLOSED_ORDER.contains(status.getShortLegStatus());
	}

	public boolean isDone() {
		return isDone(getStatus());
	}

	private boolean isDone(OrderStatusSummary status) {
		return status != null && status.longLegRemaining.compareTo(BigDecimal.ZERO) == 0 &&
				status.shortLegRemaining.compareTo(BigDecimal.ZERO) == 0;
	}

	public BigDecimal getDoneProfit() {
		return BigDecimal.ZERO;
	}
	
	public BigDecimal getEstimatedRemainingProfit() {
		return BigDecimal.ZERO;
	}
	
	private void reserveBalances(ArbInstruction arbInstruction) throws NotEnoughFundsException {
		ArbInstructionLeg longLeg = arbInstruction.getLeg(Side.BID);
		ArbInstructionLeg shortLeg = arbInstruction.getLeg(Side.ASK);
		balanceService.adiustWorkingAmount(longLeg.getMarket(), longLeg.getCurrencyPair().getQuotedCurrency(), 
				longLeg.getAmount().getQuotedAmount().negate());
		try {
			balanceService.adiustWorkingAmount(shortLeg.getMarket(), shortLeg.getCurrencyPair().getBaseCurrency(), 
					shortLeg.getAmount().getBaseAmount().negate());
		} catch (NotEnoughFundsException e) {
			// restore the long amount
			balanceService.adiustWorkingAmount(longLeg.getMarket(), longLeg.getCurrencyPair().getQuotedCurrency(), 
					longLeg.getAmount().getQuotedAmount());
			throw e;
		}
	}
	
	private void validateArbInstruction(ArbInstruction arbInstruction) {
		if (arbInstruction == null)
			throw new IllegalArgumentException("arbInstruction can not be null");
		if (arbInstruction.getLegs() == null || arbInstruction.getLeg(Side.BID) == null || arbInstruction.getLeg(Side.ASK) == null)
			throw new IllegalArgumentException("arbInstruction must have two legs");
		for (int i=0; i<2; i++) {
			ArbInstructionLeg leg = arbInstruction.getLegs().get(i ==0 ? Side.BID : Side.ASK);
			String market = leg.getMarket();
			if (market == null || !orderService.isTradingEnabled(market))
				throw new IllegalArgumentException("market " + market + " is not available for trading for leg " + leg);
			if (leg.getAmount() == null) 
				throw new IllegalArgumentException("arbInstruction has no trade amount for leg " + leg);
			if (leg.getAmount().getBaseAmount() == null || !CommonUtils.isAtLeast(leg.getAmount().getBaseAmount(), MINIMUM_AMOUNT)) 
				throw new IllegalArgumentException("arbInstruction amount is invalid for leg " + leg);
			if (leg.getPrice() == null || !CommonUtils.isAtLeast(leg.getPrice(), MINIMUM_PRICE)) 
				throw new IllegalArgumentException("arbInstruction price is invalid for leg " + leg);
			if (leg.getSide() == null)
				throw new IllegalArgumentException("arbInstruction has no side for leg " + leg);
			if (leg.getCurrencyPair() == null)
				throw new IllegalArgumentException("arbInstruction has no surrency pair for leg " + leg);
		}
	}
	
	private Tuple2<OrderStatus, OrderStatus> placeOrders(ArbInstructionLeg longLeg, ArbInstructionLeg shortLeg) {
		final AtomicReference<OrderStatus> longStatus = new AtomicReference<OrderStatus>(OrderStatus.ERROR);
		final AtomicReference<OrderStatus> shortStatus = new AtomicReference<OrderStatus>(OrderStatus.ERROR);
		int legs = 0;
		if (longLeg != null && longLeg.getAmount().getBaseAmount().compareTo(BigDecimal.ZERO) > 0)
			legs++;
		if (shortLeg != null && shortLeg.getAmount().getBaseAmount().compareTo(BigDecimal.ZERO) > 0)
			legs++;
		final CountDownLatch latch = new CountDownLatch(legs);
		if (longLeg != null && longLeg.getAmount().getBaseAmount().compareTo(BigDecimal.ZERO) > 0) {
			executionMonitorPriorityDaemon.submit(() -> {
				try {
					longStatus.set(placeOrder(longLeg.getMarket(), longLeg.getCurrencyPair(), longLeg.getAmount().getBaseAmount(), 
							longLeg.getPrice(), longLeg.getSide()));
				} finally {
					latch.countDown();
				}
			});
		} else
			longStatus.set(null);
		if (shortLeg != null && shortLeg.getAmount().getBaseAmount().compareTo(BigDecimal.ZERO) > 0) {
			executionMonitorPriorityDaemon.submit(() -> {
				try {
					shortStatus.set(placeOrder(shortLeg.getMarket(), shortLeg.getCurrencyPair(), shortLeg.getAmount().getBaseAmount(), 
							shortLeg.getPrice(), shortLeg.getSide()));
				} finally {
					latch.countDown();
				}
			});
		} else
			shortStatus.set(null);
		try {
			latch.await(10000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		}
		return new Tuple2<OrderStatus, OrderStatus>(longStatus.get(), shortStatus.get());
	}

	private OrderStatus placeOrder(String market, CurrencyPair pair, BigDecimal amount, BigDecimal price, Side side) {
		Order order = new Order(market, pair, amount, price, side);
		try {
			OrderStatus rtn = orderService.placeOrder(order);
			if (rtn != OrderStatus.OPEN)
				return rtn;
			if (order.getSide() == Side.BID)
				longOrders.add(order);
			else
				shortOrders.add(order);
			return rtn;
		} catch (Exception e) {
			logger.error("Error placing order for " + market + " " + pair + " " + amount + " " + price + " " + side, e);
			return OrderStatus.ERROR;
		}
	}

	public OrderStatusSummary getStatus() {
		ArbInstructionLeg longLeg = arbInstruction.getLeg(Side.BID); 
		ArbInstructionLeg shortLeg = arbInstruction.getLeg(Side.ASK);
		BigDecimal desiredLongLegAmount = longLeg.getAmount().getBaseAmount();
		BigDecimal desiredShortLegAmount = shortLeg.getAmount().getBaseAmount();
		Order longOrder =  longOrders.size() == 0 ? null : longOrders.get(longOrders.size() - 1); 
		Order shortOrder =  shortOrders.size() == 0 ? null : shortOrders.get(shortOrders.size() - 1); 
		OrderStatus longLegStatus = longOrder == null ? OrderStatus.INITIALISED : longOrder.getOrderStatus();
		OrderStatus shortLegStatus = shortOrder == null ? OrderStatus.INITIALISED : shortOrder.getOrderStatus();
		BigDecimal longAmount = BigDecimal.ZERO;
		for (Order order : longOrders)
			longAmount = longAmount.add(TradingUtils.getFilledAmount(order));
		BigDecimal shortAmount = BigDecimal.ZERO;
		for (Order order : shortOrders)
			shortAmount = shortAmount.add(TradingUtils.getFilledAmount(order));
		return new OrderStatusSummary(longOrder, shortOrder, longLegStatus, shortLegStatus, 
				desiredLongLegAmount, desiredShortLegAmount,
				desiredLongLegAmount.subtract(longAmount), desiredShortLegAmount.subtract(shortAmount));
	}

	private Tuple2<List<Order>, List<Order>> getOpenOrders() {
		final List<Order> longOrdersToCheck = new ArrayList<Order>();
		final List<Order> shortOrdersToCheck = new ArrayList<Order>();
		longOrders.forEach(order -> {
			if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus())) {
				logger.info("Long order to be checked {}", order);
				longOrdersToCheck.add(order);
			}
		});
		shortOrders.forEach(order -> {
			if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus())) {
				logger.info("Short order to be checked {}", order);
				shortOrdersToCheck.add(order);
			}
		});
		return new Tuple2<List<Order>, List<Order>>(longOrdersToCheck, shortOrdersToCheck);
	}

	private boolean isPriceStillThere(Side side, String market, CurrencyPair pair, BigDecimal maxPrice) {
		LiquidityEntry entry = OrderBookManager.getBestBidAsk(arbService.getOrderBook(market, pair.getName()), null);
		if (side == Side.BID) {
			if (entry != null && entry.getBidAskPrice().get(Side.ASK) != null && 
					entry.getBidAskPrice().get(Side.ASK).compareTo(maxPrice) <= 0) {
				return true;
			}
		} else {
			if (entry != null && entry.getBidAskPrice().get(Side.BID) != null && 
					entry.getBidAskPrice().get(Side.BID).compareTo(maxPrice) >= 0) {
				return true;
			}
		}
		return false;
	}
	
	private Tuple2<BigDecimal, BigDecimal> getMarketStillThere(Side side, String market, CurrencyPair pair, BigDecimal maxPrice, BigDecimal maxAmount) {
		BigDecimal maxAmountStillThere = INCREMENT;
		BigDecimal bestPrice = BigDecimal.ZERO;
		if (isPriceStillThere(side, market, pair, maxPrice)) {
			while (maxAmountStillThere.compareTo(BigDecimal.ZERO) > 0) {
				LiquidityEntry entry = OrderBookManager.getBestBidAsk(arbService.getOrderBook(market, pair.getName()), maxAmount);
				if (side == Side.BID) {
					if (entry != null && entry.getBidAskPrice().get(Side.ASK) != null && 
							entry.getBidAskPrice().get(Side.ASK).compareTo(maxPrice) <= 0) {
						maxAmountStillThere = maxAmount;
						bestPrice = entry.getBidAskPrice().get(Side.ASK);
						break;
					}
				} else {
					if (entry != null && entry.getBidAskPrice().get(Side.BID) != null && 
							entry.getBidAskPrice().get(Side.BID).compareTo(maxPrice) >= 0) {
						maxAmountStillThere = maxAmount;
						bestPrice = entry.getBidAskPrice().get(Side.BID);
						break;
					}
				}
				maxAmount = maxAmount.subtract(INCREMENT);
			}
		}
		if (!CommonUtils.isZero(bestPrice) && CommonUtils.isAtLeast(maxAmountStillThere, INCREMENT)) {
			return new Tuple2<BigDecimal, BigDecimal>(bestPrice, maxAmountStillThere);
		}
		return new Tuple2<BigDecimal, BigDecimal>(BigDecimal.ZERO, BigDecimal.ZERO); 
	}

	private void queryStatus() {
		statusLock.lock();
		try {
			Tuple2<List<Order>, List<Order>> ordersToCheck = getOpenOrders();
			List<Order> longOrdersToCheck = ordersToCheck.getA();
			List<Order> shortOrdersToCheck = ordersToCheck.getB();
			if (longOrdersToCheck.size() > 0 || shortOrdersToCheck.size() > 0) {
				int numChecks = 0;
				if (longOrdersToCheck.size() > 0)
					numChecks++;
				if (shortOrdersToCheck.size() > 0)
					numChecks++;
				CountDownLatch latch = new CountDownLatch(numChecks);
				if (longOrdersToCheck.size() > 0) {
					executionMonitorPriorityDaemon.submit(() -> {
						try {
							orderService.checkStatuses(longOrdersToCheck.get(0).getMarket(), longOrdersToCheck);
						} catch (Exception e) {
							logger.error("Error checking long order statuses for {}", longOrdersToCheck, e);
						} finally {
							latch.countDown();
						}
					});
				}
				if (shortOrdersToCheck.size() > 0) {
					executionMonitorPriorityDaemon.submit(() -> {
						try {
							orderService.checkStatuses(shortOrdersToCheck.get(0).getMarket(), shortOrdersToCheck);
						} catch (Exception e) {
							logger.error("Error checking short order statuses for {}", shortOrdersToCheck, e);
						} finally {
							latch.countDown();
						}
					});
				}
				try {
					latch.await(10000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
				}
				if (latch.getCount() > 0)
					logger.error("Awaiting order checking timed out");
			}
		} finally {
			statusLock.unlock();
		}
	}
	
	private void reviewStatus() {
		statusLock.lock();
		try {
			int tries = 0;
			while (!doReview()) {
				// wait a second and try again
				tries++;
				if (tries > 5) {
					logger.error("Tried 5 times to review arb status - suspending arbService");
					arbService.suspend(true);
					cancelAllOpenOrders();
					return;
				}
			}
			if (isClosed()) {
				logger.info("No more open orders - checking final order status");
				// check the resulting order status
				BigDecimal longsFilled = BigDecimal.ZERO;
				BigDecimal shortsFilled = BigDecimal.ZERO;
				for (Order order : longOrders)
					longsFilled = longsFilled.add(TradingUtils.getFilledAmount(order));
				for (Order order : shortOrders)
					shortsFilled = shortsFilled.add(TradingUtils.getFilledAmount(order));
				if (longsFilled.compareTo(shortsFilled) != 0) 
					logger.warn("Ended up in an imbalanced state - filled amounts differ long {} and short {}", longsFilled.toPlainString(), shortsFilled.toPlainString());
			}
		} finally {
			statusLock.unlock();
		}
	}

	private boolean doReview() {
		OrderStatusSummary status = getStatus();
		if (!isDone(status)) {
			logger.info("Current status is not done - reviewing current market");
			// check if we are on sided
			ArbInstruction arbInstruction = this.arbInstruction;
			boolean[] cancel = new boolean[]{OrderStatus.OPEN_ORDER.contains(status.longLegStatus), OrderStatus.OPEN_ORDER.contains(status.shortLegStatus)};
			boolean[] replace = new boolean[]{false, false};
			OrderBook longOrderBook = arbService.getOrderBook(status.getLongOrder().getMarket(), status.getLongOrder().getCurrencyPair().getName());
			OrderBook shortOrderBook = arbService.getOrderBook(status.getShortOrder().getMarket(), status.getShortOrder().getCurrencyPair().getName());
			logger.info("Current status is not done; reviewing status {} against long book {} and shortBook {}", status, longOrderBook, shortOrderBook);
			arbInstruction = arbService.calculateArb(longOrderBook, shortOrderBook, status.longLegRemaining, status.shortLegRemaining);
			logger.info("Updated arb instruction is {}", arbInstruction);
			switch (arbInstruction.getDecision()) {
				case NOTHING_THERE:
					if (isLopSided(status)) {
						// check if there is anything we can do
						logger.info("Fills are not balanced - checking to see if there is a price to rebalance");
						Side side = null;
						BigDecimal discrepancy = null;
						Tuple2<BigDecimal, BigDecimal> marketStillThere = null;
						if (isLopSidedLong(status)) {
							if (OrderStatus.OPEN_ORDER.contains(status.longOrder.getOrderStatus())) {
								side = Side.BID;
								discrepancy = status.longLegRemaining.subtract(status.shortLegRemaining);
								marketStillThere = this.getMarketStillThere(side, longOrderBook.getMarket(), longOrderBook.getCurrencyPair(), 
										status.shortOrder.getPrice(), discrepancy);
							}
						} else {
							if (OrderStatus.OPEN_ORDER.contains(status.shortOrder.getOrderStatus())) {
								side = Side.ASK;
								discrepancy = status.shortLegRemaining.subtract(status.longLegRemaining);
								marketStillThere = getMarketStillThere(side, shortOrderBook.getMarket(), shortOrderBook.getCurrencyPair(), 
										status.longOrder.getPrice(), discrepancy);
							}
						}
						// can we hit anything on it
						if (marketStillThere != null && !CommonUtils.isZero(marketStillThere.getA()) && !CommonUtils.isZero(marketStillThere.getB())) {
							arbInstruction = ArbInstructionFactory.createArbInstruction(this.arbInstruction, side, marketStillThere);
							logger.info("{} side can rebalance with {}", side == Side.BID ? "Long" : "Short", arbInstruction);
							if (side == Side.BID) {
								boolean doReplace = arbInstruction.getLeg(Side.BID).getPrice().compareTo(status.longOrder.getPrice()) > 0
										|| arbInstruction.getLeg(Side.BID).getAmount().getBaseAmount().compareTo(status.longOrder.getAmount()) != 0; 
								cancel = new boolean[]{doReplace, OrderStatus.OPEN_ORDER.contains(status.shortLegStatus)};
								replace = new boolean[]{doReplace, false};
							} else {
								boolean doReplace = arbInstruction.getLeg(Side.ASK).getPrice().compareTo(status.shortOrder.getPrice()) < 0
										|| arbInstruction.getLeg(Side.ASK).getAmount().getBaseAmount().compareTo(status.shortOrder.getAmount()) != 0; 
								cancel = new boolean[]{OrderStatus.OPEN_ORDER.contains(status.longLegStatus), doReplace};
								replace = new boolean[]{false, doReplace};
							}
						}
					}
					break;
				case CANCEL:
					cancel = new boolean[]{OrderStatus.OPEN_ORDER.contains(status.longLegStatus), OrderStatus.OPEN_ORDER.contains(status.shortLegStatus)};
					replace = new boolean[]{false, false};
					break;
				case HIGH:
				case LOW:
					for (int i=0; i<2; i++) {
						Side side = i==0 ? Side.BID : Side.ASK;
						ArbInstructionLeg updatedLeg = arbInstruction.getLeg(side);
						if (side == Side.BID) {
							boolean priceMoved = OrderStatus.OPEN_ORDER.contains(status.longLegStatus) && updatedLeg.getPrice().compareTo(status.longOrder.getPrice()) > 0;
							cancel[0] = priceMoved;
							replace[0] = priceMoved;
						} else {
							boolean priceMoved = OrderStatus.OPEN_ORDER.contains(status.shortLegStatus) && updatedLeg.getPrice().compareTo(status.shortOrder.getPrice()) < 0;
							cancel[1] = priceMoved;
							replace[1] = priceMoved;
						}
					}
					break;
				default:
					cancel = new boolean[]{OrderStatus.OPEN_ORDER.contains(status.longLegStatus), OrderStatus.OPEN_ORDER.contains(status.shortLegStatus)};
					replace = new boolean[]{false, false};
					break;
			}
			logger.info("Decision is long cancel={} replace={} short cancel={} replace={}", 
								cancel[0], replace[0], cancel[1], replace[1]);
			AtomicBoolean soFarSoGood = new AtomicBoolean(true);
			if (cancel[0]) {
				OrderStatus cancelStatus = cancelOrder(status.longOrder);
				soFarSoGood.compareAndSet(true, cancelStatus == OrderStatus.CANCELLED);
			}
			if (cancel[1]) {
				OrderStatus cancelStatus = cancelOrder(status.shortOrder);
				soFarSoGood.compareAndSet(true, cancelStatus == OrderStatus.CANCELLED);
			}
			if (soFarSoGood.get()) {
				if (replace[0] || replace[1]) {
					Tuple2<OrderStatus, OrderStatus> statuses = placeOrders(
							replace[0] ? arbInstruction.getLeg(Side.BID) : null,
							replace[1] ? arbInstruction.getLeg(Side.ASK) : null);
					if (replace[0])
						soFarSoGood.compareAndSet(true, statuses.getA() == OrderStatus.OPEN);
					if (replace[1])
						soFarSoGood.compareAndSet(true, statuses.getB() == OrderStatus.OPEN);
				}
			} 
			if (!soFarSoGood.get()) {
				logger.warn("Unstable execution state, there have been failures trying to adjust the position");
			}
			return soFarSoGood.get();
		}
		return true;
	}
	
	private boolean isLopSided(OrderStatusSummary status) {
		return status.longLegRemaining.subtract(status.shortLegAmount).abs().compareTo(MAXIMUM_DISCREPANCY) > 0;
	}
	
	private boolean isLopSidedLong(OrderStatusSummary status) {
		return status.longLegRemaining.compareTo(status.shortLegRemaining) > 0;
	}
	
	private void cancelAllOpenOrders() {
		for (Order order : longOrders) {
			if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus()))
				cancelOrder(order);
		}
		for (Order order : shortOrders) {
			if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus())) 
				cancelOrder(order);
		}
	}
	
	private OrderStatus cancelOrder(Order order) {
		try {
			return orderService.cancelOrder(order.getMarket(), order.getClientOrderId());
		} catch (Exception e) {
			logger.error("Error cancelling order", e);
		}
		return OrderStatus.ERROR;
	}
	
	public class StatusCheckTask implements Runnable {
		@Override
		public void run() {
			while (!isClosed() && !arbService.isSuspended()) {
				try {
					pollingLatch.await(2000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e1) {
				}
				if (pollingLatch.getCount() > 0)
					logger.warn("pollingLatch did not get counted down for 2 seconds");
				pollingLatch = new CountDownLatch(1);
				if (!isClosed() && !arbService.isSuspended()) {
					statusLock.lock();
					try {
						queryStatus();
						reviewStatus();
					} catch (Exception e) {
						logger.error("Error in status check task", e);
						arbService.suspend(true);
					} finally {
						statusLock.unlock();
					}
				}
			}
			pollingService.restorePollingInterval();
			backOfficeService.onExecutionCompletion(arbInstruction, longOrders, shortOrders, arbService.isSuspended());
		}
	}
		
	static class OrderStatusSummary {
		
		private final Order longOrder;
		private final Order shortOrder;
		private final OrderStatus longLegStatus;
		private final OrderStatus shortLegStatus;
		private final BigDecimal longLegAmount;
		private final BigDecimal shortLegAmount;
		private final BigDecimal longLegRemaining;
		private final BigDecimal shortLegRemaining;
		
		public OrderStatusSummary(Order longOrder, Order shortOrder,
				OrderStatus longLegStatus, OrderStatus shortLegStatus,
				BigDecimal longLegAmount, BigDecimal shortLegAmount,
				BigDecimal longLegRemaining, BigDecimal shortLegRemaining) {
			this.longOrder = longOrder;
			this.shortOrder = shortOrder;
			this.longLegStatus = longLegStatus;
			this.shortLegStatus = shortLegStatus;
			this.longLegAmount = longLegAmount;
			this.shortLegAmount = shortLegAmount;
			this.longLegRemaining = longLegRemaining;
			this.shortLegRemaining = shortLegRemaining;
		}

		public Order getLongOrder() {
			return longOrder;
		}

		public Order getShortOrder() {
			return shortOrder;
		}


		public OrderStatus getLongLegStatus() {
			return longLegStatus;
		}

		public OrderStatus getShortLegStatus() {
			return shortLegStatus;
		}

		public BigDecimal getLongLegAmount() {
			return longLegAmount;
		}

		public BigDecimal getShortLegAmount() {
			return shortLegAmount;
		}

		public BigDecimal getLongLegRemaining() {
			return longLegRemaining;
		}

		public BigDecimal getShortLegRemaining() {
			return shortLegRemaining;
		}

		@Override
		public String toString() {
			return "OrderStatusSummary [longOrder=" + longOrder + ", shortOrder=" + shortOrder + ", longLegStatus="
					+ longLegStatus + ", shortLegStatus=" + shortLegStatus + ", longLegAmount=" + longLegAmount
					+ ", shortLegAmount=" + shortLegAmount + ", longLegRemaining=" + longLegRemaining
					+ ", shortLegRemaining=" + shortLegRemaining + "]";
		}
		
	}

	@Override
	public void polled() {
		pollingLatch.countDown();
	}

	@Override
	public void polledException(Exception e) {
		logger.error("Uncaught exception while polling", e);
	}

}