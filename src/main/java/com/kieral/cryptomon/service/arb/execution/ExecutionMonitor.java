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
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.service.util.TradingUtils;

public class ExecutionMonitor {

	interface Sleeper {
		void sleep(long millis) throws InterruptedException;
	}
	static Sleeper sleeper = new Sleeper() {
		@Override
		public void sleep(long millis) throws InterruptedException {
			Thread.sleep(millis);
		} 
	};
	
	// Todo; pull these from a config
	private static BigDecimal MAXIMUM_DISCREPANCY = new BigDecimal("0.1");
	private static BigDecimal MINIMUM_AMOUNT = new BigDecimal("0.0001");
	private static BigDecimal MINIMUM_PRICE = new BigDecimal("0.00000001");
	private static long MINIMUM_POLLING_INTERVAL = 500;
	private static long BLOCKING_ORDER_POLLING_INTERVAL = 1000;
	
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

	private final OrderService orderService;	
	private final ArbService arbService;	
	private final BackOfficeService backOfficeService;
	private final ArbInstruction arbInstruction;
	private final List<Order> longOrders = new LinkedList<Order>();
	private final List<Order> shortOrders = new LinkedList<Order>();
	private OrderBook longOrderBook;
	private OrderBook shortOrderBook;
	private final ReentrantLock statusLock = new ReentrantLock();
	
	public ExecutionMonitor(OrderService orderService, ArbService arbService, 
			BackOfficeService backOfficeService, ArbInstruction arbInstruction, 
			long pollingIntervalMillis) {
		if (orderService == null)
			throw new IllegalArgumentException("orderService can not be null");
		if (arbService == null)
			throw new IllegalArgumentException("arbService can not be null");
		if (backOfficeService == null)
			throw new IllegalArgumentException("backOfficeService can not be null");
		if (pollingIntervalMillis < MINIMUM_POLLING_INTERVAL)
			pollingIntervalMillis = MINIMUM_POLLING_INTERVAL;
		this.orderService = orderService;
		this.arbService = arbService;
		this.backOfficeService = backOfficeService;
		this.arbInstruction = arbInstruction;
		if (arbService.isSuspended())
			throw new IllegalStateException("Not able to execute when arbService is suspended");
		validateArbInstruction(arbInstruction);
		logger.info("Executing arb instruction {}", arbInstruction);
		if (!placeOrder(arbInstruction.getLeg(Side.BID))) {
			throw new IllegalStateException("Not able to place order for " + arbInstruction.getLeg(Side.BID));
		}
		if (!placeOrder(arbInstruction.getLeg(Side.ASK))) {
			cancelOrder(longOrders.get(0));
			throw new IllegalStateException("Not able to place order for " + arbInstruction.getLeg(Side.ASK));
		}
		executionMonitorDaemon.submit(new StatusCheckTask());
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
	
	private boolean placeOrder(ArbInstructionLeg leg) {
		return placeOrder(leg.getMarket(), leg.getCurrencyPair(), leg.getAmount().getBaseAmount(), leg.getPrice(), 
				leg.getSide());
	}

	private boolean placeOrder(String market, CurrencyPair pair, BigDecimal amount, BigDecimal price, Side side) {
		Order order = new Order(market, pair, amount, price, side);
		try {
			orderService.placeOrder(order);
			if (order.getSide() == Side.BID)
				longOrders.add(order);
			else
				shortOrders.add(order);
			return true;
		} catch (Exception e) {
			logger.error("Error placing order for " + market + " " + pair + " " + amount + " " + price + " " + side, e);
			return false;
		}
	}

	private List<Order> placeMarketOrderBlocking(final Order order, long timeout) {
		final List<Order> placedOrders = new ArrayList<Order>();
		final CountDownLatch latch = new CountDownLatch(1);
		try {
			final AtomicReference<Order> orderRef = new AtomicReference<Order>(order); 
			orderService.placeMarketOrder(order);
			placedOrders.add(order);
			executionMonitorPriorityDaemon.submit(new Runnable() {
				@Override
				public void run() {
					while (latch.getCount() > 0) {
						try {
							sleeper.sleep(BLOCKING_ORDER_POLLING_INTERVAL);
						} catch (InterruptedException e) {
						}
						if (latch.getCount() > 0) {
							try {
								orderService.checkStatus(orderRef.get().getMarket(), orderRef.get().getClientOrderId());
							} catch (OrderNotExistsException e) {
							}
							OrderStatus orderStatus = orderRef.get().getOrderStatus();
							logger.info("Order status {} for balancing order {}", orderStatus, orderRef.get());
							switch (orderStatus) {
								case FILLED:
									latch.countDown();
									break;
								default:
									// replace the order
									if (OrderStatus.OPEN_ORDER.contains(orderStatus)) {
										try {
											logger.info("Cancelling unfilled balancing order {}", orderRef.get());
											orderService.cancelOrder(orderRef.get().getMarket(), orderRef.get().getClientOrderId());
										} catch (OrderNotExistsException e) {
										}
									}
									BigDecimal remainingAmount = orderRef.get().getAmount()
											.subtract(TradingUtils.getFilledAmount(orderRef.get()));
									if (remainingAmount.compareTo(MAXIMUM_DISCREPANCY) > 0) {
										Order newOrder = new Order(orderRef.get().getMarket(), orderRef.get().getCurrencyPair(),
												remainingAmount, null, orderRef.get().getSide());
										logger.info("Placing new balancing order {}", newOrder);
										orderService.placeMarketOrder(newOrder);
										placedOrders.add(newOrder);
										orderRef.set(newOrder);
									} else 
										latch.countDown();
									break;
							}
						} 
					}
				}});
			try {
				latch.await(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
			}
			return placedOrders;
		} finally {
			latch.countDown();
		}
	}
	
	public OrderStatusSummary getStatus() {
		ArbInstructionLeg longLeg = arbInstruction.getLeg(Side.BID); 
		ArbInstructionLeg shortLeg = arbInstruction.getLeg(Side.ASK);
		BigDecimal desiredLongLegAmount = longLeg.getAmount().getBaseAmount();
		BigDecimal desiredShortLegAmount = shortLeg.getAmount().getBaseAmount();
		Order longOrder =  longOrders.size() == 0 ? null : longOrders.get(longOrders.size() - 1); 
		Order shortOrder =  shortOrders.size() == 0 ? null : shortOrders.get(shortOrders.size() - 1); 
		OrderStatus longLegStatus = longOrder == null ? OrderStatus.PENDING : longOrder.getOrderStatus();
		OrderStatus shortLegStatus = shortOrder == null ? OrderStatus.PENDING : shortOrder.getOrderStatus();
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

	private void queryStatus() {
		statusLock.lock();
		try {
			longOrders.forEach(order -> {
				try {
					if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus())) {
						logger.info("Checking order status for long order {}", order);
						orderService.checkStatus(order.getMarket(), order.getClientOrderId());
					}
					longOrderBook = arbService.getOrderBook(order.getMarket(), order.getCurrencyPair().getName());
					logger.info("Latest orderbook for {} on {} {}", order.getCurrencyPair().getName(), order.getMarket(), longOrderBook);
				} catch (Exception e) {
					logger.error("Error checking order status for {}", order, e);
				}
			});
			shortOrders.forEach(order -> {
				try {
					if (OrderStatus.OPEN_ORDER.contains(order.getOrderStatus())) {
						orderService.checkStatus(order.getMarket(), order.getClientOrderId());
						logger.info("Checking order status for short order {}", order);
					}
					shortOrderBook = arbService.getOrderBook(order.getMarket(), order.getCurrencyPair().getName());
					logger.info("Latest orderbook for {} on {} {}", order.getCurrencyPair().getName(), order.getMarket(), shortOrderBook);
				} catch (Exception e) {
					logger.error("Error checking order status for {}", order, e);
				}
			});
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
				if (longsFilled.compareTo(shortsFilled) != 0) {
					logger.info("Filled amounts differ long {} and short {}", longsFilled.toPlainString(), shortsFilled.toPlainString());
					// ended up in an imbalanced state
					BigDecimal discrepancy = longsFilled.subtract(shortsFilled).abs();
					if (discrepancy.compareTo(MAXIMUM_DISCREPANCY) > 0) {
						Side side = longsFilled.compareTo(shortsFilled) > 0 ? Side.ASK : Side.BID;
						Order referenceOrder = null;
						if (side == Side.ASK) {
							if (shortOrders.size() == 0) {
								// very unexpected situation
								logger.error("No reference order available for balancing short side - suspending arbService");
								arbService.suspend(true);
							} else {
								referenceOrder = shortOrders.get(shortOrders.size() - 1);
							}
						} else {
							if (longOrders.size() == 0) {
								// very unexpected situation
								logger.error("No reference order available for balancing long side - suspending arbService");
								arbService.suspend(true);
							} else {
								referenceOrder = longOrders.get(longOrders.size() - 1);
							}
						}
						if (referenceOrder != null) {
							Order balancingOrder = new Order(referenceOrder.getMarket(), referenceOrder.getCurrencyPair(), discrepancy, null, side);
							logger.info("Placing balancing order {}", balancingOrder);
							List<Order> placedOrders = placeMarketOrderBlocking(balancingOrder, 5000);
							logger.info("Resulting balancing orders {}", placedOrders);
							if (referenceOrder.getSide() == Side.BID) 
								longOrders.addAll(placedOrders);
							else
								shortOrders.addAll(placedOrders);
							BigDecimal filledAmount = TradingUtils.getFilledAmount(placedOrders.toArray(new Order[placedOrders.size()]));
							if (balancingOrder.getAmount().subtract(filledAmount).compareTo(MAXIMUM_DISCREPANCY) > 0) {
								logger.error("Not able to fill balancing order {} - suspending arbService", balancingOrder);
								for (Order placedOrder : placedOrders) {
									if (OrderStatus.OPEN_ORDER.contains(placedOrder.getOrderStatus())) {
										try {
											logger.info("Cancelling balancing order {}", placedOrder);
											orderService.cancelOrder(placedOrder.getMarket(), placedOrder.getClientOrderId());
										} catch (Exception e) {
											logger.error("Error cancelling balancing order {}", placedOrder);
										}
									}
								}
								arbService.suspend(true);
							}
						}
					}
				}
			}
		} finally {
			statusLock.unlock();
		}
	}

	private boolean doReview() {
		OrderStatusSummary status = getStatus();
		if (!isDone(status)) {
			logger.info("Current status is not done; reviewing arb against latest orderbooks");
			ArbInstruction updatedInstruction = arbService.calculateArb(longOrderBook, shortOrderBook, status.longLegRemaining, status.shortLegRemaining);
			logger.info("Updated arb instruction is {}", updatedInstruction);
			boolean[] cancel = new boolean[]{false, false};
			boolean[] replace = new boolean[]{false, false};
			switch (updatedInstruction.getDecision()) {
				case NOTHING_THERE:
				case CANCEL:
					cancel = new boolean[]{true, true};
					replace = new boolean[]{false, false};
					break;
				case HIGH:
				case LOW:
					for (int i=0; i<2; i++) {
						Side side = i==0 ? Side.BID : Side.ASK;
						ArbInstructionLeg updatedLeg = updatedInstruction.getLeg(side);
						if (side == Side.BID) {
							if (updatedLeg.getPrice().compareTo(status.longOrder.getPrice()) > 0) {
								cancel[0] = true;
								replace[0] = true;
							}
						} else {
							if (updatedLeg.getPrice().compareTo(status.shortOrder.getPrice()) < 0) {
								cancel[1] = true;
								replace[1] = true;
							}
						}
					}
					break;
				default:
					cancel = new boolean[]{true, true};
					replace = new boolean[]{false, false};
					break;
			}
			// TODO: sanity check on cancel and no replace; have we left ourselves long on something we'd rather
			// trade out and take the loss on than be unbalanced
			logger.info("Decision is long cancel={} replace={} short cancel={} replace={}", 
								cancel[0], replace[0], cancel[1], replace[1]);
			AtomicBoolean soFarSoGood = new AtomicBoolean(true);
			if (cancel[0]) {
				boolean cancelled = cancelOrder(status.longOrder);
				soFarSoGood.compareAndSet(true, cancelled);
			}
			if (cancel[1]) {
				boolean cancelled = cancelOrder(status.shortOrder);
				soFarSoGood.compareAndSet(true, cancelled);
			}
			if (soFarSoGood.get()) {
				if (replace[0]) {
					boolean placed = placeOrder(updatedInstruction.getLeg(Side.BID));
					soFarSoGood.compareAndSet(true, placed);
				}
				if (replace[1]) {
					boolean placed = placeOrder(updatedInstruction.getLeg(Side.ASK));
					soFarSoGood.compareAndSet(true, placed);
				}
			} 
			if (!soFarSoGood.get()) {
				logger.warn("Unstable execution state, there have been failures trying to adjust the position");
			}
			return soFarSoGood.get();
		}
		return true;
	}
	
	private boolean cancelOrder(Order order) {
		if (order != null) {
			try {
				orderService.cancelOrder(order.getMarket(), order.getClientOrderId());
			} catch (Exception e) {
				logger.error("Error cancelling order", e);
				return false;
			}
		} 
		return true;
	}
	
	public class StatusCheckTask implements Runnable {
		@Override
		public void run() {
			while (!isClosed() && !arbService.isSuspended()) {
				try {
					sleeper.sleep(MINIMUM_POLLING_INTERVAL);
				} catch (InterruptedException e) {
					logger.warn("Interrupted", e);
				}
				if (!isClosed()) {
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

}