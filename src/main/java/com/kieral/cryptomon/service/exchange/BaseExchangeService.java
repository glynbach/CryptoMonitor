package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.List;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.general.ApiRequest.BodyType;
import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatusListener;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties.SubscriptionMode;
import com.kieral.cryptomon.service.liquidity.OrderBookListener;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.OrderedStreamingListener;
import com.kieral.cryptomon.streaming.OrderedStreamingEmitter;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public abstract class BaseExchangeService implements ExchangeService, OrderedStreamingListener {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
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
	
	/*
	 * Blocks until it can return result of connection
	 */
	abstract protected boolean doConnect();
	
	/*
	 * Blocks until it can return result of disconnection
	 */
	abstract protected boolean doDisconnect();

	abstract protected void subscribeMarketDataTopics();
	
	abstract protected void unsubscribeMarketDataTopics();
	
	abstract protected List<OrderBookUpdate> parsePayload(StreamingPayload streamingPayload) throws ParsingPayloadException;

	@Override
	public void onOrderedStreamingPayload(StreamingPayload streamingPayload) {
		try {
			List<OrderBookUpdate> updates = parsePayload(streamingPayload);
			if (updates != null)
				this.onOrderBookUpdate(streamingPayload.getCurrencyPair(), updates, 
						streamingPayload.getSequenceNumber(), streamingPayload.getTimeReceived());
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

	protected abstract Class<? extends OrderBookResponse> getOrderBookResponseClazz();

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

	protected void onOrderBookUpdate(CurrencyPair currencyPair, List<OrderBookUpdate> updates, long sequenceNumber, long updatesReceived) {
		if (orderBookManager != null) {
			OrderBook orderBook = orderBookManager.updateOrderBook(getName(), currencyPair, updates, 
					sequenceNumber, updatesReceived, serviceProperties.getMaxLevels());
			if (orderBookListeners != null) {
				orderBookListeners.forEach(listener -> {
					listener.onOrderBookUpdate(orderBook);
				});
			}
		}
	}

	@Override
	public boolean isTradingLocked() {
		return securityModule.isLocked();
	}

	@Override
	public boolean unlockTrading(String secretKey) {
		return securityModule.unLock(secretKey);
	}

	@Override
	public void requestBalance(boolean overrideWorkingBalance) throws BalanceRequestException {
		try {
			AccountsResponse response = getAccountsResponse();
			if (response == null)
				throw new BalanceRequestException("No balances returned from exchange");
			if (response.getAccountResponses() != null) {
				response.getAccountResponses().forEach(account -> {
					if (account.getAvailableBalance() != null) {
						balanceHandler.setConfirmedBalance(getName(), account.getAccountCuurency(), account.getAvailableBalance(), overrideWorkingBalance);
					} else {
						logger.warn("No balance returned from exchange for {}", account.getAccountCuurency());
					}
				});
			} 
		} catch (Exception e) {
			throw new BalanceRequestException("Error trying to request balances", e);
		}
	}

	protected abstract Class<? extends AccountsResponse> getAccountsResponseClazz();

	protected AccountsResponse getAccountsResponse() throws Exception {
		ApiRequest apiRequest = serviceProperties.getAccountsQuery();
		if (apiRequest.getMethod() == Method.GET)
			return getTradingResponseForGet(apiRequest, "accounts", getAccountsResponseClazz());
		else {
			return getTradingResponseForPut(apiRequest, "accounts", getAccountsResponseClazz());
		}
	}
	
	protected <T> T getTradingResponseForPut(ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws Exception {
		securityModule.appendApiPostParameterEntries(apiRequest.getPostParameters());
		logger.info("Requesting {} from POST {}", descr, apiRequest);
		HttpHeaders headers = securityModule.sign(System.currentTimeMillis(), Method.POST, apiRequest.getRequestPath(), apiRequest.getBodyAsString());
		HttpEntity<?> entity = new HttpEntity<>(apiRequest.getBodyType() == BodyType.URLENCODED ? apiRequest.getBodyAsString() : apiRequest.getPostParameters(), headers);
		ResponseEntity<? extends T> response = restTemplate.postForEntity(apiRequest.getUrl(), entity, clazz);
		logger.info("{} response {}", descr, response.getBody());
		LoggingUtils.logRawData(String.format("%s: %s: %s", getName(), descr, response.getBody()));
		return response.getBody();
	}
	
	protected <T> T getTradingResponseForGet(ApiRequest apiRequest, String descr, Class<? extends T> clazz) throws Exception {
		ApiRequest securityEnrichedUrl = new ApiRequest(apiRequest.getEndPoint(), securityModule.appendApiRequestPathEntries(apiRequest.getRequestPath()), Method.GET);
		logger.info("Requesting {} from {}", descr, securityEnrichedUrl.getUrl());
		HttpHeaders headers = securityModule.sign(System.currentTimeMillis(), Method.GET, securityEnrichedUrl.getRequestPath(), null);
		HttpEntity<?> entity = new HttpEntity<>(headers);
		ResponseEntity<? extends T> response = restTemplate.exchange(securityEnrichedUrl.getUrl(), HttpMethod.GET, entity, clazz);
		logger.info("{} response {}", descr, response.getBody());
		LoggingUtils.logRawData(String.format("%s: %s: %s", getName(), descr, response.getBody()));
		return response.getBody();
	}
}
