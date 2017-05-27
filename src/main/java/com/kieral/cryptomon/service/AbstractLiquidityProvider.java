package com.kieral.cryptomon.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.ConnectionStatus;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public abstract class AbstractLiquidityProvider implements ILiquidityProvider {

	protected Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final List<IStatusListener> statusListeners = new CopyOnWriteArrayList<IStatusListener>();
	private final List<IOrderBookListener> orderBookListeners = new CopyOnWriteArrayList<IOrderBookListener>();
	
	protected final ServiceProperties serviceProperties;
	private final OrderBookManager orderBookManager;
	
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
	}
	
	@Override
	public void connect() throws InterruptedException {
		while (status.get() == ConnectionStatus.CONNECTED) {
			disconnect();
			if (status.get() == ConnectionStatus.CONNECTED)
				throw new IllegalStateException("Could not discconect from " + getName() + " - check logs");
		}
		while (!(status.get() == ConnectionStatus.CONNECTED)) {
			logInfo("Connecting to " + getName());
			if (doConnect()) {
				setStatus(ConnectionStatus.CONNECTED);
				retryCount = 0;
				logInfo("Connected to " + getName());
			} else {
				retryCount++;
				Thread.sleep((retryCount % 5 == 0) ? longSleep : shortSleep);
			}
		} 
	}

	@Override
	public void disconnect() {
		if (!(status.get() == ConnectionStatus.DISCONNECTED)) {
			logInfo("Disconnecting from " + getName());
			if (doDisconnect()) {
				setStatus(ConnectionStatus.DISCONNECTED);
				logInfo("Disconnected from " + getName());
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
					logger.warn("Error notifiying status listener " + listener + " of " + status, e);
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

	abstract protected List<OrderBookUpdate> parsePayload(StreamingPayload streamingPayload) throws ParsingPayloadException;

	protected void onPayloadUpdate(StreamingPayload streamingPayload) {
		try {
			List<OrderBookUpdate> updates = parsePayload(streamingPayload);
			if (updates != null)
				this.onOrderBookUpdate(streamingPayload.getCurrencyPair(), updates);
		} catch (ParsingPayloadException e) {
			logger.error("Error parsing payload", e);
			this.disconnect();
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
	
	protected void logInfo(String msg) {
		logger.info(msg);
	}

	protected void logWarn(String msg) {
		logger.warn(msg);
	}

	protected void logError(String msg) {
		logger.error(msg);
	}
	
	protected void logDebug(String msg) {
		logger.debug(msg);
	}
	
}
