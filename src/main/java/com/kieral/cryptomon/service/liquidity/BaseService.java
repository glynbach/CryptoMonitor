package com.kieral.cryptomon.service.liquidity;

import java.io.IOException;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.ServiceProperties;
import com.kieral.cryptomon.service.ServiceProperties.SubscriptionMode;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.IOrderedStreamingListener;
import com.kieral.cryptomon.streaming.OrderedStreamingEmitter;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public abstract class BaseService implements IService, IOrderedStreamingListener {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "ServiceScheduler");
			thread.setDaemon(true);
			return thread;
		}});
	private ScheduledFuture<?> marketDataPoller;
	private final List<IStatusListener> statusListeners = new CopyOnWriteArrayList<IStatusListener>();
	private final List<IOrderBookListener> orderBookListeners = new CopyOnWriteArrayList<IOrderBookListener>();
	
	protected final ServiceProperties serviceProperties;
	protected final OrderBookManager orderBookManager;
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
	
	protected BaseService(final ServiceProperties serviceProperties, final OrderBookManager orderBookManager) {
		if (serviceProperties == null)
			throw new IllegalArgumentException("serviceProperties can not be null");
		if (orderBookManager == null)
			throw new IllegalArgumentException("orderBookManager can not be null");
		this.serviceProperties = serviceProperties;
		this.orderBookManager = orderBookManager;
		this.skipHeartbeats = serviceProperties.isSkipHearbeats();
		this.snapshotBaseline = serviceProperties.isSnapshotBaseline();
		this.streamingEmitter = new OrderedStreamingEmitter(getName(), this, 
									snapshotBaseline, serviceProperties.isSnapshotBaselineSequence(), 4);
	}

	public String getName() {
		return serviceProperties.getExchange();
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
			for (IStatusListener listener : statusListeners) {
				try {
					listener.onStatusChange(status);
				} catch (Exception e) {
					logger.warn("Error notifiying status listener {} of {}", listener, status, e);
				}
			}
		}
	}
	
	@Override
	public void registerOrderBookListener(IOrderBookListener orderBookListener) {
		if (!orderBookListeners.contains(orderBookListener))
			orderBookListeners.add(orderBookListener);
	}

	@Override
	public void registerStatusListener(IStatusListener statusListener) {
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
					List<OrderBook> orderBooks = null;
					try {
						orderBooks = getOrderBookSnapshots(serviceProperties.getPairs());
					} catch (Exception e) {
						logger.error("Error requesting order book snapshots", e);
					}
					if (orderBooks != null) {
						orderBooks.forEach(orderBook -> {
							try {
								orderBookListeners.forEach(listener -> {
									listener.onOrderBookUpdate(orderBook);
								});
							} catch (Exception e) {
								logger.error("Error processing order book snapshots {}", orderBook, e);
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
	
	abstract protected OrderBook getOrderBookSnapshot(CurrencyPair pair) throws JsonProcessingException, IOException;
	
	abstract protected List<OrderBook> getOrderBookSnapshots(List<CurrencyPair> pairs) throws JsonProcessingException, IOException;

	abstract protected List<OrderBookUpdate> parsePayload(StreamingPayload streamingPayload) throws ParsingPayloadException;

	@Override
	public void onOrderedStreamingPayload(StreamingPayload streamingPayload) {
		try {
			List<OrderBookUpdate> updates = parsePayload(streamingPayload);
			if (updates != null)
				this.onOrderBookUpdate(streamingPayload.getCurrencyPair(), updates);
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
				OrderBook orderBookSnapshot = getOrderBookSnapshot(streamingPayload.getCurrencyPair());
				this.streamingEmitter.onSnashotUpdate(orderBookSnapshot);
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

	protected void onOrderBookUpdate(CurrencyPair currencyPair, List<OrderBookUpdate> updates) {
		if (orderBookManager != null) {
			OrderBook orderBook = orderBookManager.updateOrderBook(getName(), currencyPair, updates);
			if (orderBookListeners != null) {
				orderBookListeners.forEach(listener -> {
					listener.onOrderBookUpdate(orderBook);
				});
			}
		}
	}
	
	
	
}
