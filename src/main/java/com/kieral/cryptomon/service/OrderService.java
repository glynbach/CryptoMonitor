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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.trading.OpenOrderStatus;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

@Component
@EnableScheduling
public class OrderService {

	private static final AtomicLong orderIdCounter = new AtomicLong(Instant.now().getEpochSecond());
	private static final AtomicInteger counter = new AtomicInteger(0);

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
					asyncProcessor.submit(() -> {
						try {
							exchange.updateBalances(true);
						} catch (Exception e) {
							logger.error("Error updating balances for " + exchange.getName());
						}
						List<Order> openOrders = exchange.getOpenOrders();
						if (openOrders != null) {
							openOrders.forEach(order -> {
								updateStatus(order, order.getOrderStatus(), null);
							});
						}
					});
				}
			});
		});
	}

	public void registerOrderListener(OrderListener listener) {
		if (listener != null && !listeners.contains(listener))
			listeners.add(listener);
	}
	
	public List<Order> getClosedOrders(String market) {
		return closedOrders.containsKey(market) ? new ArrayList<Order>(closedOrders.get(market).values()) : Collections.emptyList();
	}

	public List<Order> getOpenOrders(String market) {
		return openOrders.containsKey(market) ? new ArrayList<Order>(openOrders.get(market).values()) : Collections.emptyList();
	}

	public List<Order> getAllOrders(String market) {
		List<Order> rtn = new ArrayList<Order>();
		rtn.addAll(getOpenOrders(market));
		rtn.addAll(getClosedOrders(market));
		return rtn;
	}
	
	public void placeOrder(Order order) {
		if (order.getMarket() == null)
			throw new IllegalArgumentException("market can not be null");
		if (order.getAmount() == null)
			throw new IllegalArgumentException("amount can not be null");
		if (order.getCurrencyPair() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (order.getPrice() == null)
			throw new IllegalArgumentException("price can not be null");
		if (order.getSide() == null)
			throw new IllegalArgumentException("side can not be null");
		if (order.getOrderStatus() == null)
			order.setOrderStatus(OrderStatus.PENDING);
		if (order.getOrderStatus() != OrderStatus.PENDING)
			throw new IllegalStateException(String.format("Cannot place an order with status %s", order.getOrderStatus()));
		if (order.getClientOrderId() == null)
			order.setClientOrderId(generateClientOrderId());
		if (order.getCreatedTime() == 0)
			order.setCreatedTime(System.currentTimeMillis());
		updateStatus(order, order.getOrderStatus());
		try {
			updateStatus(order, exchangeManagerService.placeOrder(order));
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

	public OrderStatus forceCancelOrder(String market, String orderId) throws OrderNotExistsException {
		logger.info("Received force cancel order for exchange orderId {}", orderId);
		return exchangeManagerService.forceCancelOrder(market, orderId);
	}

	private void updateStatus(Order order, OrderStatus orderStatus) {
		updateStatus(order, orderStatus, null);
	}

	private void updateStatus(Order order, OrderStatus orderStatus, BigDecimal amountRemaining) {
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
		if (amountRemaining != null)
			order.setAmountRemaining(amountRemaining);
		listeners.forEach( listener -> {
			listener.onOrderStatusChange(order);
		});
		logger.info("DEBUG openOrders: " + openOrders + " closedOrders: "+ closedOrders);
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
		Map<String, OpenOrderStatus> statuses = exchangeManagerService.getOpenOrderStatuses(market, orders);
		if (statuses != null) {
			statuses.values().forEach(status -> {
				updateStatus(status.getOrder(), status.getNewStatus(), status.getAmountRemaining());
			});
		}
	}
	
	private boolean contains(ConcurrentMap<String, Map<String, Order>> map, Order order) {
		return map.containsKey(order.getMarket()) && map.get(order.getMarket()).containsKey(order.getClientOrderId());
	}

	private boolean contains(ConcurrentMap<String, Map<String, Order>> map, String market, String exchangeOrderId) {
		if (map.containsKey(market)) {
			for (Order order : map.get(market).values()) {
				if (order.getOrderId().equals(exchangeOrderId))
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
