package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangeService;

public class OrderServiceImpl implements OrderService {

	private static final AtomicLong orderIdCounter = new AtomicLong(Instant.now().getEpochSecond());
	private static final AtomicInteger counter = new AtomicInteger(0);
	private static final BigDecimal MARKET_ORDER_PRICE = new BigDecimal("-1");
	private static final int MAX_BALANCE_RETRIES = 3;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final List<OrderListener> listeners = new CopyOnWriteArrayList<OrderListener>();
	private final ExecutorService asyncProcessor = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "OrderServiceAsyncProcessor-" + counter.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
		
	});
	
	private final ConcurrentMap<String, Map<String, Order>> openOrders = new ConcurrentHashMap<String, Map<String, Order>>();
	private final ConcurrentMap<String, Map<String, Order>> closedOrders = new ConcurrentHashMap<String, Map<String, Order>>();
	private final ConcurrentMap<String, AtomicInteger> attempts =  new ConcurrentHashMap<String, AtomicInteger>();

	@Autowired
	private ExchangeManagerService exchangeManagerService;
	@Autowired
	private BalanceService balanceService;

    private final AtomicBoolean initialised = new AtomicBoolean(false);

	@PostConstruct
	public void init() {
		if (initialised.compareAndSet(false, true)) {
			logger.info("Initialising");
			exchangeManagerService.getEnabledExchanges().forEach(exchange -> {
				logger.info("Registering trading status listener for {}", exchange.getName());
				exchange.registerTradingStatusListener(enabled -> {
					if (enabled) {
						logger.info("Exchange {} enabled for trading", exchange.getName());
						asyncProcessor.submit(() -> {
							try {
								List<Order> openOrders = exchange.getOpenOrders();
								if (openOrders != null) {
									openOrders.forEach(order -> {
										updateStatus(order, order.getOrderStatus());
									});
								}
							} catch (Exception e) {
								logger.warn("Error requesting open orders", e);
							}
							requestBalances(exchange, 0, true);
						});
					}
				});
			});
		}
	}

	@Override
	public void requestAllBalances() {
		synchronized(this) {
			exchangeManagerService.getEnabledExchanges().forEach(exchange -> {
				requestBalances(exchange, 0, true);
			});
		}
	}

	public boolean requestBalances(String market) {
		ExchangeService exchange = exchangeManagerService.getEnabledExchange(market);
		if (exchange != null)
			return requestBalances(exchange, 0, true);
		return false;
	}
	

	private boolean requestBalances(ExchangeService exchange, long delay, boolean reset) {
		attempts.putIfAbsent(exchange.getName() + ":BALANCE", new AtomicInteger(0));
		if (reset)
			attempts.get(exchange.getName() + ":BALANCE").set(0);
		if (delay > 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {}
		}
		try {
			exchange.updateBalances(true);
			Set<Currency> currencies = new HashSet<Currency>();
			exchange.getProperties().getPairs().forEach(pair -> {
				currencies.add(pair.getBaseCurrency());
				currencies.add(pair.getQuotedCurrency());
			});
			currencies.forEach(currency -> {
				if (balanceService.getConfirmedAmount(exchange.getName(), currency) == null) {
					balanceService.setConfirmedBalance(exchange.getName(), currency, BigDecimal.ZERO, true);
				}
			});
			logger.info("Balances updated for {} - latest balances {}", exchange.getName(), balanceService.getPrettyPrint());
			return true;
		} catch (Exception e) {
			logger.error("Error updating balances for " + exchange.getName(), e);
			// resubmit with delay
			if (attempts.get(exchange.getName() + ":BALANCE").incrementAndGet() < MAX_BALANCE_RETRIES)
				requestBalances(exchange, 1000, false);
		}
		return false;
	}

	@Override
	public boolean isTradingEnabled(String market) {
		return exchangeManagerService.isTradingEnabled(market);
	}

	@Override
	public void registerOrderListener(OrderListener listener) {
		if (listener != null && !listeners.contains(listener))
			listeners.add(listener);
	}
	
	@Override
	public List<Order> getClosedOrders(String market) {
		return closedOrders.containsKey(market) ? new ArrayList<Order>(closedOrders.get(market).values()) : Collections.emptyList();
	}

	@Override
	public List<Order> getOpenOrders(String market) {
		return openOrders.containsKey(market) ? new ArrayList<Order>(openOrders.get(market).values()) : Collections.emptyList();
	}

	@Override
	public Order getOrder(String market, String clientOrderId) {
		if (openOrders.containsKey(market) && openOrders.get(market).containsKey(clientOrderId))
			return openOrders.get(market).get(clientOrderId);
		if (closedOrders.containsKey(market))
			return closedOrders.get(market).get(clientOrderId);
		return null;
	}

	@Override
	public List<Order> getAllOrders(String market) {
		List<Order> rtn = new ArrayList<Order>();
		rtn.addAll(getOpenOrders(market));
		rtn.addAll(getClosedOrders(market));
		return rtn;
	}

	@Override
	public OrderStatus placeMarketOrder(Order order) {
		order.setPrice(MARKET_ORDER_PRICE);
		return placeOrder(order);
	}

	@Override
	public OrderStatus placeOrder(Order order) {
		if (order.getMarket() == null)
			throw new IllegalArgumentException("market can not be null");
		if (order.getAmount() == null)
			throw new IllegalArgumentException("amount can not be null");
		if (order.getCurrencyPair() == null && order.getCurrencyPairStr() == null)
			throw new IllegalArgumentException("both currencyPair and currencyPairStr can not be null");
		if (order.getPrice() == null)
			throw new IllegalArgumentException("price can not be null");
		if (order.getSide() == null)
			throw new IllegalArgumentException("side can not be null");
		if (order.getOrderStatus() == null)
			order.setOrderStatus(OrderStatus.INITIALISED);
		if (order.getOrderStatus() != OrderStatus.INITIALISED)
			throw new IllegalStateException(String.format("Cannot place an order with status %s", order.getOrderStatus()));
		if (order.getCurrencyPair() == null) {
			CurrencyPair currencyPair = exchangeManagerService.getCurrencyPair(order.getMarket(), order.getCurrencyPairStr());
			if (currencyPair == null) {
				throw new IllegalStateException(String.format("Cannot find currency pair for %s", order.getCurrencyPairStr()));
			}
		}
		if (order.getClientOrderId() == null)
			order.setClientOrderId(generateClientOrderId());
		if (order.getCreatedTime() == 0)
			order.setCreatedTime(System.currentTimeMillis());
		updateStatus(order, OrderStatus.SENDING);
		try {
			OrderStatus status;
			if (order.getPrice() == MARKET_ORDER_PRICE)
				status = exchangeManagerService.placeMarketOrder(order);
			else
				status = exchangeManagerService.placeOrder(order);
			if (order.getOrderId() == null)
				throw new IllegalStateException("No orderId returned from placing order");
			updateStatus(order, status);
			return status;
		} catch (Exception e) {
			logger.error("Uncaught error attempting to place order {} - checking open orders to see if it has been placed", order, e);
			// if the service could not handle the exception then we don't know the outcome of the order; check all known open orders
			try {
				List<Order> openOrders = exchangeManagerService.getOpenOrders(order.getMarket());
				if (openOrders != null) {
					for (Order openOrder : openOrders) {
						if (!contains(this.openOrders, order.getMarket(), openOrder.getOrderId())) {
							if (!contains(this.closedOrders, order.getMarket(), openOrder.getOrderId())) {
								logger.info("Found open order {} we have no record of", openOrder);
								if (openOrder.getMarket().equals(order.getMarket()) &&
										openOrder.getCurrencyPair().getName().equals(order.getCurrencyPair().getName()) &&
										openOrder.getSide() == order.getSide() &&
										openOrder.getAmount().compareTo(order.getAmount()) == 0 &&
										openOrder.getPrice().compareTo(order.getPrice()) == 0) {
									logger.info("open order {} is a match to {}", openOrder, order);
									order.setOrderId(openOrder.getOrderId());
									updateStatus(order, openOrder.getOrderStatus());
									return openOrder.getOrderStatus();
								}
							} 
						}
					}
					// all open orders accounted for
					logger.info("No open orders found we don't know about - marking {} as cancelled", order);
					updateStatus(order, OrderStatus.CANCELLED);
				}
				return OrderStatus.CANCELLED;
			} catch (Exception ex) {
				// TODO: we need to send an SMS alert or attempt a full recovery
				logger.error("Unable to check open orders - we now have an order {} with an unknown state", order);
				order.setMessage("Error placing order - unknown status");
				updateStatus(order, OrderStatus.ERROR);
				throw ex;
			}
		}
	}

	@Override
	public OrderStatus cancelOrder(String market, String clientOrderId) throws OrderNotExistsException {
		Order order = get(openOrders, market, clientOrderId);
		if (order == null) {
			order = get(closedOrders, market, clientOrderId);
			if (order != null) {
				logger.warn("Received cancel for already cancelled order {} - sending cancel anyway", order);
				OrderStatus status = exchangeManagerService.cancelOrder(order);
				updateStatus(order, status);
				return status;
			} else {
				throw new OrderNotExistsException(String.format("No order exists to cancel for clientOrderId {}", clientOrderId));
			}
		} else {
			OrderStatus status = exchangeManagerService.cancelOrder(order);
			updateStatus(order, status);
			return status;
		}
	}

	@Override
	public OrderStatus forceCancelOrder(String market, String orderId) throws OrderNotExistsException {
		logger.info("Received force cancel order for exchange orderId {}", orderId);
		return exchangeManagerService.forceCancelOrder(market, orderId);
	}

	private void updateStatus(Order order, OrderStatus orderStatus) {
		if (order == null || order.getOrderStatus() == orderStatus)
			return;
		logger.info("Updating order status to {} for order {}", orderStatus, order);
		if (OrderStatus.OPEN_ORDER.contains(orderStatus)) {
			if (OrderStatus.CLOSED_ORDER.contains(order.getOrderStatus())) {
				if (orderStatus == OrderStatus.ERROR) {
					// do nothing; order is already closed
					return;
				}
				logger.warn("Received update status of {} but order already closed with status of {}", orderStatus, order.getOrderStatus());
			}
			remove(closedOrders, order);
			put(openOrders, order);
		} else {
			if (contains(openOrders, order)) {
				order.setClosedTime(System.currentTimeMillis());
				remove(openOrders, order);
			}
			put(closedOrders, order);
		}
		order.setOrderStatus(orderStatus);
		logger.info("Current open orders {}", openOrders);
		listeners.forEach( listener -> {
			listener.onOrderStatusChange(order);
		});
	}
	
	@Override
	public void checkStatus(String market, String clientOrderId) throws OrderNotExistsException {
		Order order = get(openOrders, market, clientOrderId);
		if (order != null) {
			checkStatuses(market, Arrays.asList(new Order[]{order}));
		} else {
			order = get(closedOrders, market, clientOrderId);
			if (order == null)
				throw new OrderNotExistsException(String.format("No order found for %s on market %s", clientOrderId, market));
			logger.warn("Received check status for already cancelled order {} - checking status anyway", order);
			checkStatuses(market, Arrays.asList(new Order[]{order}));
		}
	}

	@Override
	public void checkStatuses(String market, List<Order> orders) {
		Map<String, Order> orderMap = orders.stream().collect(Collectors.toMap(Order::getClientOrderId, Function.identity()));
		Map<String, OrderStatus> statuses = exchangeManagerService.getOpenOrderStatuses(market, orders);
		if (statuses != null) {
			statuses.keySet().forEach(clientOrderId -> {
				if (orderMap.containsKey(clientOrderId))
					updateStatus(orderMap.get(clientOrderId), statuses.get(clientOrderId));
			});
		}
	}
	
	private boolean contains(ConcurrentMap<String, Map<String, Order>> map, Order order) {
		return map.containsKey(order.getMarket()) && map.get(order.getMarket()).containsKey(order.getClientOrderId());
	}

	private boolean contains(ConcurrentMap<String, Map<String, Order>> map, String market, String exchangeOrderId) {
		if (map.containsKey(market)) {
			for (Order order : map.get(market).values()) {
				if (order.getOrderId() != null && order.getOrderId().equals(exchangeOrderId))
					return true;
			}
		};
		return false;
	}

	private void put(ConcurrentMap<String, Map<String, Order>> map, Order order) {
		String marketId = order.getMarket();
		String clientOrderId = order.getClientOrderId();
		map.putIfAbsent(marketId, new LinkedHashMap<String, Order>());
		map.get(order.getMarket()).put(clientOrderId, order);
	}

	private void remove(ConcurrentMap<String, Map<String, Order>> map, Order order) {
		if (map.containsKey(order.getMarket()))
			map.get(order.getMarket()).remove(order.getClientOrderId());
	}

	private Order get(ConcurrentMap<String, Map<String, Order>> map, String marketId, String clientOrderId) {
		if (map.containsKey(marketId))
			return map.get(marketId).get(clientOrderId);
		return null;
	}

	public static String generateClientOrderId() {
		return "OrderId-" + orderIdCounter.incrementAndGet();
	}
	
}
