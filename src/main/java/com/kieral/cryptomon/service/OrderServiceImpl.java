package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

@Component
@EnableScheduling
public class OrderServiceImpl implements OrderService {

	private static final AtomicLong orderIdCounter = new AtomicLong(Instant.now().getEpochSecond());
	private static final AtomicInteger counter = new AtomicInteger(0);
	private static final BigDecimal MARKET_ORDER_PRICE = new BigDecimal("-1");

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

	@Autowired
	private ExchangeManagerService exchangeManagerService;

	@PostConstruct
	public void init() {
		exchangeManagerService.getEnabledExchanges().forEach(exchange -> {
			exchange.registerTradingStatusListener(enabled -> {
				if (enabled) {
					logger.info("Exchange {} enabled for trading", exchange.getName());
					asyncProcessor.submit(() -> {
						try {
							exchange.updateBalances(true);
						} catch (Exception e) {
							logger.error("Error updating balances for " + exchange.getName());
						}
						List<Order> openOrders = exchange.getOpenOrders();
						if (openOrders != null) {
							openOrders.forEach(order -> {
								updateStatus(order, order.getOrderStatus());
							});
						}
					});
				}
			});
		});
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
	public void placeMarketOrder(Order order) {
		order.setPrice(MARKET_ORDER_PRICE);
		placeOrder(order);
	}

	@Override
	public void placeOrder(Order order) {
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
			order.setOrderStatus(OrderStatus.PENDING);
		if (order.getOrderStatus() != OrderStatus.PENDING)
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
		updateStatus(order, order.getOrderStatus());
		try {
			OrderStatus status;
			if (order.getPrice() == MARKET_ORDER_PRICE)
				status = exchangeManagerService.placeMarketOrder(order);
			else
				status = exchangeManagerService.placeOrder(order);
			if (order.getOrderId() == null)
				throw new IllegalStateException("No orderId returned from placing order");
			updateStatus(order, status);
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
									return;
								}
							} 
						}
					}
					// all open orders accounted for
					logger.info("No open orders found we don't know about - marking {} as cancelled", order);
					updateStatus(order, OrderStatus.CANCELLED);
				}
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
	public void cancelOrder(String market, String clientOrderId) throws OrderNotExistsException {
		Order order = get(openOrders, market, clientOrderId);
		if (order == null) {
			order = get(closedOrders, market, clientOrderId);
			if (order != null) {
				logger.warn("Received cancel for already cancelled order {} - sending cancel anyway", order);
				updateStatus(order, exchangeManagerService.cancelOrder(order));
			} else {
				throw new OrderNotExistsException(String.format("No order exists to cancel for clientOrderId {}", clientOrderId));
			}
		} else {
			updateStatus(order, exchangeManagerService.cancelOrder(order));
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
	
    @Scheduled(fixedRate = 1000)
    public void checkOpenOrders() {
    	// TODO: this might be a bit spammy - it might be better to request updates from arb manager
//    	openOrders.keySet().forEach(market -> {
//    		List<Order> openOrders = getOpenOrders(market);
//    		if (openOrders.size() > 0) {
//    			logger.info("Checking status of open orders on {}", market);
//    			Map<String, OpenOrderStatus> statuses = exchangeManagerService.getOpenOrderStatuses(market, openOrders);
//    			if (statuses != null) {
//    				statuses.values().forEach(status -> {
//    					updateStatus(status.getOrder(), status.getNewStatus(), status.getAmountRemaining());
//    				});
//    			}
//    		}
//    	});
    }
    
	@Override
	public void checkStatus(String market, String clientOrderId) throws OrderNotExistsException {
		Order order = get(openOrders, market, clientOrderId);
		if (order != null) {
			checkStatuses(market, Arrays.asList(new Order[]{order}));
		} else {
			final Order closedOrder = get(closedOrders, market, clientOrderId);
			if (closedOrder == null)
				throw new OrderNotExistsException(String.format("No order found for %s on market %s", clientOrderId, market));
			listeners.forEach( listener -> {
				listener.onOrderStatusChange(closedOrder);
			});
		}
	}

	private void checkStatuses(String market, List<Order> orders) {
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
