package com.kieral.cryptomon.service.liquidity;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.ServiceProperties;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.IOrderedStreamingListener;
import com.kieral.cryptomon.streaming.OrderedStreamingEmitter;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public abstract class AbstractLiquidityProvider implements ILiquidityProvider, IOrderedStreamingListener {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final List<IStatusListener> statusListeners = new CopyOnWriteArrayList<IStatusListener>();
	private final List<IOrderBookListener> orderBookListeners = new CopyOnWriteArrayList<IOrderBookListener>();
	
	protected final ServiceProperties serviceProperties;
	protected final OrderBookManager orderBookManager;
	private final OrderedStreamingEmitter streamingEmitter;
	
	private final ConcurrentMap<String, AtomicBoolean> initialisedStreams = new ConcurrentHashMap<String, AtomicBoolean>();
	
	private final boolean skipEmptyUpdates;
	private final boolean requiresSnapshot; 
	
	private final AtomicReference<ConnectionStatus> status = new AtomicReference<ConnectionStatus>();
	{
		status.set(ConnectionStatus.DISCONNECTED);
	}

	private final long shortSleep = 5 * 1000;
	private final long longSleep = 30 * 1000;
	private int retryCount = 0;
	
	protected AbstractLiquidityProvider(final ServiceProperties serviceProperties, final OrderBookManager orderBookManager) {
		this.serviceProperties = serviceProperties;
		this.orderBookManager = orderBookManager;
		this.skipEmptyUpdates = serviceProperties != null && serviceProperties.isSipValidationOnEmptyPayloads();
		this.requiresSnapshot = serviceProperties != null && serviceProperties.isRequiresSnapshot();
		this.streamingEmitter = new OrderedStreamingEmitter(getName(), this, requiresSnapshot, 4);
	}
	
	@Override
	public void connect() throws InterruptedException {
		while (status.get() == ConnectionStatus.CONNECTED) {
			disconnect();
			if (status.get() == ConnectionStatus.CONNECTED)
				throw new IllegalStateException(String.format("Could not discconect from %s - check logs", getName()));
		}
		while (!(status.get() == ConnectionStatus.CONNECTED)) {
			logger.info("Connecting to %s", getName());
			if (doConnect()) {
				setStatus(ConnectionStatus.CONNECTED);
				retryCount = 0;
				logger.info("Connected to %s", getName());
			} else {
				retryCount++;
				Thread.sleep((retryCount % 5 == 0) ? longSleep : shortSleep);
			}
		} 
	}

	@Override
	public void disconnect() {
		if (!(status.get() == ConnectionStatus.DISCONNECTED)) {
			logger.info("Disconnecting from %s", getName());
			if (doDisconnect()) {
				setStatus(ConnectionStatus.DISCONNECTED);
				logger.info("Disconnected from %s", getName());
			}
		}
	}

	private void setStatus(ConnectionStatus status) {
		if (status == ConnectionStatus.CONNECTED && this.status.compareAndSet(ConnectionStatus.DISCONNECTED, status))
			subscribeMarketDataTopics();
		else
			this.status.set(status);
		if (statusListeners != null) {
			for (IStatusListener listener : statusListeners) {
				try {
					listener.onStatusChange(status);
				} catch (Exception e) {
					logger.warn(String.format("Error notifiying status listener %s of %s", listener, status)
							, e);
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

	abstract protected String getName();
	
	/*
	 * Blocks until it can return result of connection
	 */
	abstract protected boolean doConnect();
	
	/*
	 * Blocks until it can return result of disconnection
	 */
	abstract protected boolean doDisconnect();

	abstract protected void subscribeMarketDataTopics();
	
	abstract protected OrderBook subscribeOrderbookSnapshot(String topic, String currencyPair) throws JsonProcessingException, IOException;

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
		logger.error(String.format("Error on streaming topic %s: %s", topic, reason));
		// TODO: re-subscribe topic
	}

	protected void onPayloadUpdate(StreamingPayload streamingPayload) {
		LoggingUtils.logRawData(
				String.format("%s: streaming: %s", getName(), streamingPayload.getRaw()));
		AtomicBoolean initialised = initialisedStreams.putIfAbsent(streamingPayload.getTopic(), new AtomicBoolean(!requiresSnapshot));
		if (requiresSnapshot && (initialised == null || !initialised.get())) {
			logger.info("Received update on %s - requesting snapshot", streamingPayload.getTopic());
			try {
				OrderBook orderBookSnapshot = subscribeOrderbookSnapshot(streamingPayload.getTopic(), streamingPayload.getCurrencyPair());
				this.streamingEmitter.onSnashotUpdate(orderBookSnapshot);
				initialisedStreams.get(streamingPayload.getTopic()).set(true);
			} catch (Exception e) {
				logger.error(
						String.format("Error subscriving to orderbook snapshot for %s", streamingPayload.getTopic()), 
						e);
				// TODO: resubscribe topic instead
				this.disconnect();
			}
		}
		if (!skipEmptyUpdates || (streamingPayload.getJson().size() > 0)) {
			this.streamingEmitter.onStreamingUpdate(streamingPayload);
		}
	}

	protected void onOrderBookUpdate(String currencyPair, List<OrderBookUpdate> updates) {
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
