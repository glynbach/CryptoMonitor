package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.general.ApiRequest.BodyType;
import com.kieral.cryptomon.model.general.ApiRequest.ResponseErrorAction;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatusListener;
import com.kieral.cryptomon.service.exception.ApiRequestException;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.exception.ExpectedResponseException;
import com.kieral.cryptomon.service.exception.SecurityModuleException;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties.SubscriptionMode;
import com.kieral.cryptomon.service.liquidity.OrderBookListener;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.rest.TradesResponse;
import com.kieral.cryptomon.service.rest.OrderResponse.RequestNature;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.service.util.TradingUtils;
import com.kieral.cryptomon.streaming.OrderedStreamingListener;
import com.kieral.cryptomon.streaming.OrderedStreamingEmitter;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public abstract class BaseExchangeService implements ExchangeService, OrderedStreamingListener {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());

	protected enum OrderCheckingStrategy {
		CHECK_BY_INDIVIDUAL,
		CHECK_BY_OPEN_ORDERS,
		CHECK_BY_TRADE_HISTORY
	}
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ServiceScheduler-" + getName());
			thread.setDaemon(true);
			return thread;
		}});
	private ScheduledFuture<?> marketDataPoller;
	private final List<ConnectionStatusListener> statusListeners = new CopyOnWriteArrayList<ConnectionStatusListener>();
	private final List<OrderBookListener> orderBookListeners = new CopyOnWriteArrayList<OrderBookListener>();
	private final List<TradingStatusListener> tradingStatusListeners = new CopyOnWriteArrayList<TradingStatusListener>();

	@Autowired
	protected OrderBookManager orderBookManager;
	@Autowired
	protected BalanceService balanceHandler;
	@Autowired
	protected RestTemplate restTemplate;

	protected final ServiceExchangeProperties serviceProperties;
	protected final ServiceSecurityModule securityModule;
	private final OrderedStreamingEmitter streamingEmitter;
	
	private final ConcurrentMap<String, AtomicBoolean> initialisedStreams = new ConcurrentHashMap<String, AtomicBoolean>();
	
	private final boolean skipHeartbeats;
	private final boolean snapshotBaseline; 

	private final AtomicReference<ConnectionStatus> status = new AtomicReference<ConnectionStatus>();
	{
		status.set(ConnectionStatus.DISCONNECTED);
	}

	private final long shortSleep = 5 * 1000;
	private final long longSleep = 30 * 1000;
	private int retryCount = 0;
	
	protected BaseExchangeService(final ServiceExchangeProperties serviceProperties, final ServiceSecurityModule securityModule) {
		if (serviceProperties == null)
			throw new IllegalArgumentException("serviceProperties can not be null");
		if (securityModule == null)
			throw new IllegalArgumentException("securityModule can not be null");
		logger.info("Creating service with properties {}", serviceProperties);
		this.serviceProperties = serviceProperties;
		this.securityModule = securityModule;
		this.skipHeartbeats = serviceProperties.isSkipHearbeats();
		this.snapshotBaseline = serviceProperties.isSnapshotBaseline();
		this.streamingEmitter = new OrderedStreamingEmitter(getName(), this, 
									snapshotBaseline, serviceProperties.isSnapshotBaselineSequence(), 4);
	}

	@Override
	public String getName() {
		return serviceProperties.getExchange();
	}
	
	@Override
	public boolean isEnabled() {
		return serviceProperties.isEnabled();
	}
	
	@Override
	public void connect() throws InterruptedException {
		while (status.get() == ConnectionStatus.CONNECTED) {
			disconnect();
			if (status.get() == ConnectionStatus.CONNECTED)
				throw new IllegalStateException(String.format("Could not discconect from %s - check logs", getName()));
		}
		while (!(status.get() == ConnectionStatus.CONNECTED)) {
			logger.info("Connecting to {}", getName());
			if (serviceProperties.getSubscriptionMode() == SubscriptionMode.POLLING || doConnect()) {
				setStatus(ConnectionStatus.CONNECTED);
				retryCount = 0;
				logger.info("Connected to {}", getName());
			} else {
				retryCount++;
				Thread.sleep((retryCount % 5 == 0) ? longSleep : shortSleep);
			}
		} 
	}

	@Override
	public void disconnect() {
		if (!(status.get() == ConnectionStatus.DISCONNECTED)) {
			logger.info("Disconnecting from {}", getName());
			if (serviceProperties.getSubscriptionMode() == SubscriptionMode.POLLING || doDisconnect()) {
				setStatus(ConnectionStatus.DISCONNECTED);
				logger.info("Disconnected from {}", getName());
			}
		}
	}

	private void setStatus(ConnectionStatus status) {
		if (status == ConnectionStatus.CONNECTED && this.status.compareAndSet(ConnectionStatus.DISCONNECTED, status)) {
			subscribeMarketData();
		} else if (status == ConnectionStatus.DISCONNECTED && this.status.compareAndSet(ConnectionStatus.CONNECTED, status)) {
			unsubscribeMarketData();
		} else
			this.status.set(status);
		
		if (statusListeners != null) {
			for (ConnectionStatusListener listener : statusListeners) {
				try {
					listener.onStatusChange(status);
				} catch (Exception e) {
					logger.warn("Error notifiying status listener {} of {}", listener, status, e);
				}
			}
		}
	}
	
	@Override
	public void registerOrderBookListener(OrderBookListener orderBookListener) {
		if (!orderBookListeners.contains(orderBookListener))
			orderBookListeners.add(orderBookListener);
	}

	@Override
	public void registerStatusListener(ConnectionStatusListener statusListener) {
		if (!statusListeners.contains(statusListener))
			statusListeners.add(statusListener);
	}

	@Override
	public void registerTradingStatusListener(TradingStatusListener tradingStatusListener) {
		if (!tradingStatusListeners.contains(tradingStatusListener))
			tradingStatusListeners.add(tradingStatusListener);
	}

	private void subscribeMarketData() {
		if (serviceProperties.getSubscriptionMode() == SubscriptionMode.STREAMING)
			subscribeMarketDataTopics();
		else {
			long delay = serviceProperties.getPollingInterval() <= 0 ? 2000L : serviceProperties.getPollingInterval(); 
			marketDataPoller = scheduler.scheduleWithFixedDelay(new Runnable(){
				@Override
				public void run() {
					List<OrderBookResponse> orderBookResponses = null;
					try {
						orderBookResponses = getOrderBookResponses(serviceProperties.getPairs());
					} catch (Exception e) {
						logger.error("Error requesting order book snapshots", e);
					}
					if (orderBookResponses != null) {
						orderBookResponses.forEach(orderBookResponse -> {
							try {
								OrderBook orderBook = orderBookManager.getOrderBook(orderBookResponse, 
										getName(), orderBookResponse.getCurrencyPair(), serviceProperties.getMaxLevels());
								orderBookListeners.forEach(listener -> {
									listener.onOrderBookUpdate(orderBook);
								});
							} catch (Exception e) {
								logger.error("Error processing order book response {}", orderBookResponse, e);
							}
						});
					}
				}}, delay, delay, TimeUnit.MILLISECONDS);
		}
	}
	
	private void unsubscribeMarketData() {
		if (serviceProperties.getSubscriptionMode() == SubscriptionMode.STREAMING)
			unsubscribeMarketDataTopics();
		else {
			if (marketDataPoller != null) {
				marketDataPoller.cancel(true);
			}
		}
	}
	
	@Override
	public void onOrderedStreamingPayload(StreamingPayload streamingPayload) {
		try {
			List<OrderBookUpdate> updates = parsePayload(streamingPayload);
			if (updates != null)
				this.onOrderBookUpdate(streamingPayload.getCurrencyPair(), updates, 
						streamingPayload.getSequenceNumber(), streamingPayload.getTimeReceived(), true);
		} catch (ParsingPayloadException e) {
			logger.error("Error parsing payload", e);
			// TODO: resubscribe topic instead
			this.disconnect();
		}
	}

	@Override
	public void onOrderedStreamingError(String topic, String reason) {
		logger.error("Error on streaming topic {}: {}", topic, reason);
		// TODO: re-subscribe topic
	}

	protected void onPayloadUpdate(StreamingPayload streamingPayload) {
		LoggingUtils.logRawData(
				String.format("%s: streaming: %s", getName(), streamingPayload.getRaw()));
		AtomicBoolean initialised = initialisedStreams.putIfAbsent(streamingPayload.getCurrencyPair().getTopic(), new AtomicBoolean(!snapshotBaseline));
		if (snapshotBaseline && (initialised == null || !initialised.get())) {
			logger.info("Received update on {} - requesting snapshot", streamingPayload.getCurrencyPair().getTopic());
			try {
				OrderBookResponse orderBookResponse = getOrderBookResponse(streamingPayload.getCurrencyPair());
				this.streamingEmitter.onSnashotUpdate(orderBookManager.getOrderBook(orderBookResponse,
						getName(), streamingPayload.getCurrencyPair(), serviceProperties.getMaxLevels()));
				initialisedStreams.get(streamingPayload.getCurrencyPair().getTopic()).set(true);
			} catch (Exception e) {
				logger.error("Error subscribing to orderbook snapshot for {}", streamingPayload.getCurrencyPair().getTopic(), e);
				// TODO: resubscribe topic instead
				this.disconnect();
			}
		}
		if (!skipHeartbeats || !streamingPayload.isHeartbeat()) {
			this.streamingEmitter.onStreamingUpdate(streamingPayload);
		}
	}

	protected OrderBookResponse getOrderBookResponse(CurrencyPair currencyPair) {
		ApiRequest url = serviceProperties.getOrderBookSnapshotQuery(currencyPair.getTopic());
		if (logger.isDebugEnabled())
			logger.debug("Requesting orderbook snapshot from {}", url.getUrl());
		OrderBookResponse response = restTemplate.getForObject(url.getUrl(), getOrderBookResponseClazz());
		response.setCurrencyPair(currencyPair);
		if (logger.isDebugEnabled())
			logger.debug("Orderbook snapshot response {}", response);
		LoggingUtils.logRawData(String.format("%s: snapshot: %s", getName(), response));
		return response;
	}

	protected List<OrderBookResponse> getOrderBookResponses(List<CurrencyPair> pairs) {
		if (pairs == null)
			return null;
		List<OrderBookResponse> orderBookResponses = new ArrayList<OrderBookResponse>();
		for (CurrencyPair pair : pairs) {
			OrderBookResponse orderBookResponse = getOrderBookResponse(pair);
			if (orderBookResponse != null)
				orderBookResponses.add(orderBookResponse);
		}
		return orderBookResponses;
	}

	protected void onOrderBookUpdate(CurrencyPair currencyPair, List<OrderBookUpdate> updates, long sequenceNumber, long updatesReceived, boolean valid) {
		if (orderBookManager != null) {
			OrderBook orderBook = orderBookManager.updateOrderBook(getName(), currencyPair, updates, 
					sequenceNumber, updatesReceived, valid, serviceProperties.getMaxLevels());
			if (orderBookListeners != null) {
				orderBookListeners.forEach(listener -> {
					listener.onOrderBookUpdate(orderBook);
				});
			}
		}
	}

	@Override
	public boolean isTradingEnabled() {
		return securityModule.isEnabled();
	}

	@Override
	public boolean enableTrading(String secretKey) {
		boolean enabled = securityModule.enable(secretKey);
		tradingStatusListeners.forEach(listener -> {
			listener.onTradingEnabled(enabled);
		});
		if (enabled) {
			// TODO - DEBUG REMOVE THIS
			Thread thread = new Thread() {
				public void run() {
					try {
						Thread.sleep(6000);
						if (BaseExchangeService.this.getName().equals("gdax"))
							BaseExchangeService.this.cancelOrder("bb4ef795-2be9-4d20-9f5c-549de0a5b3ec");
					} catch (Exception e) {
						logger.error("Error cancelling order 3d0fb74e-e4b5-43b5-98cd-a79cdfb1381", e);
					}
				}
			};
			thread.start();
		}
		return enabled;
	}

	@Override
	public void updateBalances(boolean overrideWorkingBalance) throws BalanceRequestException {
		try {
			AccountsResponse response = getAccountsResponse();
			if (response == null)
				throw new BalanceRequestException("No balances returned from exchange");
			if (response.getAccountResponses() != null) {
				response.getAccountResponses().forEach(account -> {
					if (account.getAvailableBalance() != null) {
						if (serviceProperties.isInterestingCurrency(account.getAccountCurrency()))
							balanceHandler.setConfirmedBalance(getName(), account.getAccountCurrency(), account.getAvailableBalance(), overrideWorkingBalance);
					} else {
						logger.warn("No balance returned from exchange for {}", account.getAccountCurrency());
					}
				});
			} 
		} catch (Exception e) {
			throw new BalanceRequestException("Error trying to request balances", e);
		}
	}

	
	@Override
	public OrderStatus placeOrder(Order order) {
		if (order == null)
			throw new IllegalArgumentException("order can not be null");
		ApiRequest apiRequest = null;
		try {
			apiRequest = serviceProperties.getPlaceOrderQuery(order.getSide(), order.getCurrencyPair(), 
				order.getPrice(), new TradeAmount(order.getAmount(), order.getPrice(), order.getCurrencyPair().getPriceScale()));
		} catch (Exception e) {
			logger.error("Error placing order {}", order, e);
			// failure to create apiRequest, will not have been sent to the exchange
			order.setMessage("Creating API request error");
			return OrderStatus.CANCELLED;
		}
		try {
			PlaceOrderResponse response = getTradingResponse(apiRequest, "place order", getPlaceOrderResponseClazz());
			OrderStatus orderStatus = response == null ? null : response.getOrderStatus(PlaceOrderResponse.class, order.getOrderStatus());
			if (orderStatus == null || orderStatus == OrderStatus.ERROR) {
				logger.error("Received an invalid order status in exchange response {} for order {}", response, order);
				order.setMessage("ERROR PLACING ORDER - STATUS UNKNOWN");
				return OrderStatus.ERROR;
			} 
			String orderId = response.getOrderId();
			if (OrderStatus.OPEN_ORDER.contains(orderStatus)) {
				if (orderId == null) {
					logger.error("Received an open order status in exchange response {} for order {} but can detect an orderId", response, order);
					order.setMessage("ERROR - COULD NOT GET THE ORDERID");
					return OrderStatus.ERROR;
				} 
			} 
			if (orderId != null)
				order.setOrderId(orderId);
			return orderStatus;
		} catch (SecurityModuleException e) {
			logger.error("Error placing order {}", order, e);
			// failure to sign, will not have been sent to the exchange
			order.setMessage("Signing payload error");
			return OrderStatus.CANCELLED;
		} catch (ApiRequestException e) {
			logger.error("Error placing order {}", order, e);
			// failure to marshal body, will not have been sent to the exchange
			order.setMessage("Generating body exception");
			return OrderStatus.CANCELLED;
		} catch (ExpectedResponseException e) {
			if (ResponseErrorAction.CANCEL == e.getAction()) {
				logger.error("Error response {} {} when placing order {} - marking ac cancelled", e.getStatus(), e.getResponseBody(), order);
				order.setMessage(e.getResponseBody());
				return OrderStatus.CANCELLED;
			} else {
				logger.error("Error response {} {} when placing order {} - marking ac error", e.getStatus(), e.getResponseBody(), order);
				order.setMessage(e.getResponseBody());
				return OrderStatus.ERROR;
			} 
		} 
	}

	@Override
	public OrderStatus cancelOrder(Order order) {
		if (order == null)
			throw new IllegalArgumentException("order can not be null");
		AtomicReference<String> message = new AtomicReference<String>();
		OrderStatus newOrderStatus = cancelOrder(order.getOrderId(), order.getOrderStatus(), message);
		if (message.get() != null)
			order.setMessage(message.get());
		return newOrderStatus;
	}

	@Override
	public OrderStatus cancelOrder(String orderId) {
		return cancelOrder(orderId, OrderStatus.ERROR, null);
	}

	private OrderStatus cancelOrder(String orderId, OrderStatus currentOrderStatus, AtomicReference<String> message) {
		if (message == null)
			message = new AtomicReference<String>();
		if (orderId == null) {
			logger.error("Been asked to cancel an order with no orderId and current status {}", currentOrderStatus);
			message.set("Cannot cancel order with no orderId");
		}
		ApiRequest apiRequest = null;
		try {
			apiRequest = serviceProperties.getCancelOrderQuery(orderId);
		} catch (Exception e) {
			logger.error("Error getting api request for cancel order {}", orderId, e);
			// failure to create apiRequest, will not have been sent to the exchange
			message.set("Creating API request error");
			return currentOrderStatus;
		}
		try {
			CancelOrderResponse response = getTradingResponse(apiRequest, "cancel order", getCancelOrderResponseClazz());
			OrderStatus orderStatus = response == null ? null : response.getOrderStatus(CancelOrderResponse.class, currentOrderStatus);
			if (orderStatus == null || orderStatus == OrderStatus.ERROR) {
				logger.error("Received an invalid order status in exchange response {} for cancel orderId {}", response, orderId);
				message.set("ERROR CANCELLING ORDER - STATUS UNKNOWN");
				return OrderStatus.ERROR;
			} 
			return orderStatus;
		} catch (SecurityModuleException e) {
			logger.error("Error cancelling orderId {}", orderId, e);
			// failure to sign, will not have been sent to the exchange
			message.set("Signing payload error");
			return currentOrderStatus;
		} catch (ApiRequestException e) {
			logger.error("Error cancelling orderId {}", orderId, e);
			// failure to marshal body, will not have been sent to the exchange
			message.set("Generating body exception");
			return currentOrderStatus;
		} catch (ExpectedResponseException e) {
			if (ResponseErrorAction.CANCEL == e.getAction()) {
				logger.info("Response with status code {} and body {} for cancellation of {} can be marked as cancelled", e.getStatus(), e.getResponseBody(), orderId);
				message.set(e.getResponseBody());
				return OrderStatus.CANCELLED;
			} else {
				logger.info("Response with status code {} and body {} for cancellation of {} can not be cancelled", e.getStatus(), e.getResponseBody(), orderId);
				return currentOrderStatus;
			} 
		}
	}
	
	@Override
	public Map<String, OrderStatus> getOpenOrderStatuses(List<Order> orders) {
		Map<String, OrderStatus> rtn = new HashMap<String, OrderStatus>();
		if (orders != null) {
			logger.info("Getting open order statuses for {}", orders);
			List<Order> ordersForChecking = new ArrayList<Order>(orders);
			AtomicBoolean failed = new AtomicBoolean(false);
			getOrderCheckingStrategies().forEach(strategy -> {
				if (ordersForChecking.size() > 0) {
					Map<String, OrderStatus> statuses = null;
					switch (strategy) {
						case CHECK_BY_INDIVIDUAL:
							statuses = getOpenOrderStatusesByIndividualOrder(orders, failed);
							logger.info("Statuses returned by checking individual order statuses {}", statuses);
							break;
						case CHECK_BY_OPEN_ORDERS:
							statuses = this.getOpenOrderStatusesByOpenOrders(orders, failed);
							logger.info("Statuses returned by checking open orders {}", statuses);
							break;
						case CHECK_BY_TRADE_HISTORY:
							statuses = getOpenOrderStatusesByHistory(orders, failed);
							logger.info("Statuses returned by checking trade history {}", statuses);
							break;
					}
					if (statuses != null) {
						Iterator<Order> i = ordersForChecking.iterator();
						while (i.hasNext()) {
							Order order = i.next();
							if (statuses.containsKey(order.getClientOrderId())) {
								logger.info("Order status {} resolved by strategy {}", statuses.get(order.getClientOrderId()), strategy.name());
								rtn.put(order.getClientOrderId(), statuses.get(order.getClientOrderId()));
								i.remove();
							}
						}
					}
				}
			});
			if (ordersForChecking.size() > 0) {
				if (failed.get()) {
					logger.warn("Unable to assess the remaining orders as there was a failure in checking statuses");
				} else {
					// we've checked if they belong to live open orders and checked if there are any statuses associated
					// with being filled, so can assume they have been cancelled without fills
					logger.info("{} orders are neither in open orders list or filled order history so setting as cancelled");
					ordersForChecking.forEach(order -> {
						rtn.put(order.getClientOrderId(), OrderStatus.CANCELLED);
					});
				}
			}
		}
		logger.info("Returning resolved statuses {}", rtn);
		return rtn;
	}

    Function<Order, CurrencyPair> currencyPairFromOrder = new Function<Order, CurrencyPair>() {
    	public CurrencyPair apply(Order order) {
    		return order.getCurrencyPair();
    	}
    };

	private Map<String, OrderStatus> getOpenOrderStatusesByIndividualOrder(List<Order> orders, AtomicBoolean failed) {
		Map<String, OrderStatus> rtn = new HashMap<String, OrderStatus>();
		if (orders != null) {
			orders.forEach(order -> {
				if (order != null) {
					if (order.getOrderId() == null) {
						// without an exchange orderId there is nothing we can do to further assess the status
						rtn.put(order.getClientOrderId(), order.getOrderStatus());
					} else {
						OrderResponse orderResponse = null;
						try {
							orderResponse = getOrderResponse(order);
						} catch (ExpectedResponseException e) {
							// no action
						} catch (Exception e) {
							failed.set(true);
							logger.error("Error getting order response for {}", order, e);
						}
						if (orderResponse != null) {
							if (orderResponse.isSuccess()) {
								OrderStatus orderStatus = orderResponse.getOrderStatus(RequestNature.UNKNOWN_ORDER_RESPONSES, order);
								try {
									merge(order, orderResponse.getTradeResponses());
								} catch (Exception e) {
									order.setMessage("Trade merge failed - check status");
								}
								rtn.put(order.getClientOrderId(), orderStatus);
							} else {
								failed.set(true);
							}
						}
					}
				}
			});
		}
		return rtn;
	}

	private Map<String, OrderStatus> getOpenOrderStatusesByHistory(List<Order> orders, AtomicBoolean failed) {
		Map<String, OrderStatus> rtn = new HashMap<String, OrderStatus>();
		if (orders != null) {
			Map<String, Order> ordersToCheck = orders.stream().collect(Collectors.toMap(Order::getOrderId, Function.identity()));
			List<CurrencyPair> currencyPairsForChecking = new ArrayList<CurrencyPair>(
					orders.stream().map(currencyPairFromOrder).collect(Collectors.<CurrencyPair>toSet()));
			currencyPairsForChecking.forEach(pair -> {
				OrdersResponse<? extends OrderResponse> response = null;
				try {
					response = getOrderHistoryResponse(pair);
				} catch (ExpectedResponseException e) {
					// no action
				} catch (Exception e) {
					failed.set(true);
					logger.error("Error getting order history response for {}", pair, e);
				}
				if (response != null) {
					if (response.isSuccess()) {
						if (response.getOrderResponses() != null) {
							response.getOrderResponses().forEach(orderResponse -> {
								if (orderResponse.isSuccess() && ordersToCheck.containsKey(orderResponse.getOrderId())) {
									Order order = ordersToCheck.get(orderResponse.getOrderId());
									OrderStatus orderStatus = orderResponse.getOrderStatus(RequestNature.CLOSED_ORDER_RESPONSE, order);
									try {
										merge(order, orderResponse.getTradeResponses());
									} catch (Exception e) {
										order.setMessage("Trade merge failed - check status");
									}
									rtn.put(order.getClientOrderId(), orderStatus);
									ordersToCheck.remove(order.getOrderId());
								}
							});
						}
					} else { 
						failed.set(true);
					}
				}
			});
		}
		return rtn;
	}

	private Map<String, OrderStatus> getOpenOrderStatusesByOpenOrders(List<Order> orders, AtomicBoolean failed) {
		Map<String, OrderStatus> rtn = new HashMap<String, OrderStatus>();
		if (orders != null) {
			Map<String, Order> ordersToCheck = orders.stream().collect(Collectors.toMap(Order::getOrderId, Function.identity()));
			List<CurrencyPair> currencyPairsForChecking = new ArrayList<CurrencyPair>(
					orders.stream().map(currencyPairFromOrder).collect(Collectors.<CurrencyPair>toSet()));
			currencyPairsForChecking.forEach(pair -> {
				OrdersResponse<? extends OrderResponse> response = null;
				try {
					response = getOpenOrdersResponse(pair);
				} catch (ExpectedResponseException e) {
					// no action
				} catch (Exception e) {
					failed.set(true);
					logger.error("Error getting open orders response for {}", pair, e);
				}
				if (response != null) {
					if (response.isSuccess()) {
						if (response.getOrderResponses() != null) {
							response.getOrderResponses().forEach(orderResponse -> {
								if (orderResponse.isSuccess() && ordersToCheck.containsKey(orderResponse.getOrderId())) {
									Order order = ordersToCheck.get(orderResponse.getOrderId());
									OrderStatus orderStatus = orderResponse.getOrderStatus(RequestNature.OPEN_ORDER_RESPONSE, order);
									try {
										this.merge(order, orderResponse.getTradeResponses());
									} catch (Exception e) {
										order.setMessage("Trade merge failed - check status");
									}
									rtn.put(order.getClientOrderId(), orderStatus);
									ordersToCheck.remove(order.getOrderId());
								}
							});
						}
					} else {
						failed.set(true);
					}
				}
			});
		}
		return rtn;
	}

	private void merge(Order order, TradesResponse tradesResponse) {
		if (order == null || tradesResponse == null || tradesResponse.getNumTrades() == 0)
			return;
		if (tradesResponse.isPlaceholder() || !serviceProperties.isHasGranularTrades()) {
			order.replaceTrades(tradesResponse.getTrades());
		} else {
			List<Trade> trades = tradesResponse.getTrades();
			List<String> newTradeIds = trades.size() == 0 ? Collections.emptyList() :
				trades.stream().map(Trade::getTradeId).collect(Collectors.<String>toList());
			if (newTradeIds.contains(null)) {
				logger.warn("Merge called on {} containing null tradeIds); Calling replace instead", trades);
				order.replaceTrades(trades);
			} else
				order.mergeTrades(trades);
		}
	}
	
	@Override
	public List<Order> getOpenOrders() {
		if (serviceProperties.getPairs() == null)
			return null;
		List<Order> rtn = new ArrayList<Order>();
		serviceProperties.getPairs().forEach(pair -> {
			OrdersResponse<? extends OrderResponse> response = null;
			try {
				response = getOpenOrdersResponse(pair);
			} catch (Exception e) {
				logger.error("Error getting open orders response for {}", pair, e);
			}
			if (response != null && response.getOrderResponses() != null) {
				response.getOrderResponses().forEach(orderResponse -> {
					if (orderResponse.isSuccess()) {
						// create a new order from this order response
						rtn.add(new Order(getName(), pair.getName(), pair, orderResponse.getAmount(), orderResponse.getPrice(),
								orderResponse.getSide(), 
								TradingUtils.getOrderStatus(RequestNature.OPEN_ORDER_RESPONSE, true, orderResponse.getAmount(), orderResponse.getTradeResponses()), 
								OrderService.generateClientOrderId(), 
								orderResponse.getOrderId(), orderResponse.getCreatedTime(), orderResponse.getCreatedTime(),
								"Requested from exchange"));
					}
				});
			}
		});
		return rtn;
	}

	protected OrdersResponse<? extends OrderResponse> getOrderHistoryResponse(CurrencyPair currencyPair) throws Exception {
		ApiRequest apiRequest = serviceProperties.getOrderHistoryQuery(currencyPair);
		return getTradingResponse(apiRequest, "order history query", getOrderHistoryResponseClazz());	
	}

	protected OrdersResponse<? extends OrderResponse> getOpenOrdersResponse(CurrencyPair currencyPair) throws Exception {
		ApiRequest apiRequest = serviceProperties.getOpenOrdersQuery(currencyPair);
		return getTradingResponse(apiRequest, "open orders query", getOpenOrdersResponseClazz());	
	}

	protected OrderResponse getOrderResponse(Order order) throws Exception {
		ApiRequest apiRequest = serviceProperties.getOrderQuery(order.getOrderId());
		return getTradingResponse(apiRequest, "order query", getOrderResponseClazz());	
	}

	protected AccountsResponse getAccountsResponse() throws Exception {
		ApiRequest apiRequest = serviceProperties.getAccountsQuery();
		return getTradingResponse(apiRequest, "accounts", getAccountsResponseClazz());	
	}

	private <T> T getTradingResponse(ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws SecurityModuleException, 
																								ApiRequestException, ExpectedResponseException {
		if (apiRequest.getMethod() == HttpMethod.POST)
			return getTradingResponseForPut(apiRequest, descr, clazz);
		else {
			// this works for GET or DELETE
			return getTradingResponseForMethod(apiRequest, descr, clazz);
		}
	}
	
	private <T> T getTradingResponseForPut(ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws SecurityModuleException, ApiRequestException, ExpectedResponseException {
		securityModule.appendApiPostParameterEntries(apiRequest.getPostParameters());
		logger.info("Requesting {} from POST {}", descr, apiRequest);
		HttpHeaders headers = securityModule.sign(System.currentTimeMillis(), apiRequest.getMethod(), apiRequest.getRequestPath(), apiRequest.getBodyAsString());
		HttpEntity<?> entity = new HttpEntity<>(apiRequest.getBodyType() == BodyType.URLENCODED ? apiRequest.getBodyAsString() : apiRequest.getPostParameters(), headers);
		return processResponse(apiRequest.getUrl(), entity, apiRequest, descr, clazz);
	}
	
	private <T> T getTradingResponseForMethod(ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws SecurityModuleException, ExpectedResponseException {
		ApiRequest securityEnrichedUrl = new ApiRequest(apiRequest.getEndPoint(), securityModule.appendApiRequestPathEntries(apiRequest.getRequestPath()), apiRequest.getMethod());
		logger.info("Requesting {} from {}", descr, securityEnrichedUrl.getUrl());
		HttpHeaders headers = securityModule.sign(System.currentTimeMillis(), apiRequest.getMethod(), securityEnrichedUrl.getRequestPath(), null);
		HttpEntity<?> entity = new HttpEntity<>(headers);
		return processResponse(securityEnrichedUrl.getUrl(), entity, apiRequest, descr, clazz);
	}
	
	private <T> T processResponse(String url, HttpEntity<?> entity, ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws ExpectedResponseException {
		try {
			ResponseEntity<? extends T> response = null;
			if (HttpMethod.POST == apiRequest.getMethod())
				response = restTemplate.postForEntity(apiRequest.getUrl(), entity, clazz);
			else
				response = restTemplate.exchange(url, apiRequest.getMethod(), entity, clazz);
			logger.info("{} response {}", descr, response.getBody());
			LoggingUtils.logRawData(String.format("%s: %s: %s", getName(), descr, response.getBody()));
			return response.getBody();
		} catch (HttpClientErrorException e) {
			ResponseErrorAction action = apiRequest.checkResponseError(e.getStatusCode(), e.getResponseBodyAsString());
			if (action != null && action != ResponseErrorAction.RAISE_EXCEPTION) {
				if (logger.isDebugEnabled())
					logger.debug("{} is expected by apiRequest {}", e.getStatusCode(), apiRequest);
				throw new ExpectedResponseException(e, action, e.getStatusCode(), e.getResponseBodyAsString());
			} else {
				logger.error("Received rest exception with status {} and response body {}", e.getStatusCode(), e.getResponseBodyAsString());
				throw e;
			}
		}
	}
	
	@Override
	public CurrencyPair getCurrencyPair(String currencyPairStr) {
		if (currencyPairStr != null) {
			for (CurrencyPair pair : serviceProperties.getPairs()) {
				if (currencyPairStr.equals(pair.getName()))
					return pair;
			}
		}
		return null;
	}

	@Override
	public ServiceExchangeProperties getProperties() {
		return serviceProperties;
	}

	/*
	 * Blocks until it can return result of connection
	 */
	abstract protected boolean doConnect();
	/*
	 * Blocks until it can return result of disconnection
	 */
	abstract protected boolean doDisconnect();
	/*
	 * Subscribes to streaming market data topic
	 */
	abstract protected void subscribeMarketDataTopics();
	/*
	 * Unsubscribes from streaming market data topic
	 */
	abstract protected void unsubscribeMarketDataTopics();
	/*
	 * Parses the payload of a streaming market data topic
	 */
	abstract protected List<OrderBookUpdate> parsePayload(StreamingPayload streamingPayload) throws ParsingPayloadException;
	/*
	 * Returns POJOs for JSon responses
	 */
	abstract protected Class<? extends OrderBookResponse> getOrderBookResponseClazz();
	abstract protected Class<? extends AccountsResponse> getAccountsResponseClazz();
	abstract protected Class<? extends PlaceOrderResponse> getPlaceOrderResponseClazz();
	abstract protected Class<? extends CancelOrderResponse> getCancelOrderResponseClazz();
	abstract protected Class<? extends OrdersResponse<? extends OrderResponse>> getOpenOrdersResponseClazz();
	abstract protected Class<? extends OrdersResponse<? extends OrderResponse>> getOrderHistoryResponseClazz();
	abstract protected Class<? extends OrderResponse> getOrderResponseClazz();
	/**
	 * Get permitted order checking strategy for exchange in priority order 
	 */
	abstract protected EnumSet<OrderCheckingStrategy> getOrderCheckingStrategies();

}
