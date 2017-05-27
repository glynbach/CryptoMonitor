package com.kieral.cryptomon.service.poloniex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.ConnectionStatus;
import com.kieral.cryptomon.model.OrderBookAction;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.AbstractLiquidityProvider;
import com.kieral.cryptomon.service.OrderBookManager;
import com.kieral.cryptomon.service.ServiceProperties;
import com.kieral.cryptomon.service.SubscriptionProperties;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;
import com.kieral.cryptomon.streaming.StreamingProperties;
import com.kieral.cryptomon.streaming.StreamingProvider;
import com.kieral.cryptomon.streaming.wamp.WampStreamingProvider;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class PoloniexService extends AbstractLiquidityProvider {

	private static final String POLONIEX = "Poloniex";

	private final StreamingProvider streamingProvider = new WampStreamingProvider();
	private final StreamingProperties streamingProperties;

	private AtomicLong lastSequence = new AtomicLong(-1);
	
	public PoloniexService(ServiceProperties serviceProperties, OrderBookManager orderBookManager) {
		super(serviceProperties, orderBookManager);
		if (serviceProperties == null)
			throw new IllegalArgumentException("serviceProperties can not be null");
		streamingProperties = new StreamingProperties.Builder()
				.uri(serviceProperties.getUri())
				.realm("realm1")
				.build();
	}
	
	@Override
	protected String getName() {
		return POLONIEX;
	}

	@Override
	protected boolean doConnect() {
		final AtomicBoolean connected = new AtomicBoolean();
		lastSequence.set(-1);
		Completable streamingConnection = streamingProvider.connect(streamingProperties, status -> {
			if (status == ConnectionStatus.CONNECTED) 
				connected.set(true);
			else
				connected.set(false);
		});
		// give it 30 seconds to try
		streamingConnection.blockingAwait(30, TimeUnit.SECONDS);
		if (!connected.get())
			streamingProvider.disconnect();
		return connected.get();
	}

	@Override
	protected boolean doDisconnect() {
		Completable completable = streamingProvider.disconnect();
		completable.blockingAwait(5, TimeUnit.SECONDS);
		return true;
	}

	@Override
	protected void subscribeMarketDataTopics() {
		if (serviceProperties.getMarketDataTopics() != null)
			serviceProperties.getMarketDataTopics().forEach(subscription -> {
				Observable<StreamingPayload> result = streamingProvider.subscribe(subscription.getTopic(), subscription.getTopic());
				result.subscribe(streamingPayload -> {
					onPayloadUpdate(streamingPayload);
				});
			});
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		List<OrderBookUpdate> updates = new ArrayList<OrderBookUpdate>();
		JsonNode json = payload.getJson();
		logger.info("Received payload: " + payload);
		long lastSequenceNumber = lastSequence.getAndSet(payload.getSequenceNumber()); 
		try {
			json.elements().forEachRemaining(node -> {
				logger.info("DEBUG: node element: " + node);
				JsonNode type = node.findValue("type");
				if (type != null && type.isTextual()) {
					String updateType = type.asText();
					if (updateType.equalsIgnoreCase("orderBookModify") || updateType.equalsIgnoreCase("orderBookRemove")) {
						// check sequence number
//						if (lastSequenceNumber >= payload.getSequenceNumber())
//							throw new IllegalStateException("Invald sequence number " + payload.getSequenceNumber() + " have already processed " + lastSequenceNumber);
						OrderBookAction action = updateType.equalsIgnoreCase("orderBookModify") ? OrderBookAction.REPLACE : OrderBookAction.REMOVE; 
						JsonNode data = node.findValue("data");
						if (data == null)
							throw new IllegalStateException("Received " + updateType + " with no data in node " + node + " from payload " + payload);
						String orderBookType = data.findValue("type").asText();
						String orderBookRate = data.findValue("rate").asText();
						if (orderBookType == null || orderBookType.isEmpty()) {
							throw new IllegalStateException("Received " + updateType + " with no order type in node " + node + " from payload " + payload);
						}
						if (orderBookRate == null || orderBookRate.isEmpty()) {
							throw new IllegalStateException("Received " + updateType + " with no rate in node " + node + " from payload " + payload);
						}
						String orderBookAmount = null;
						if (action == OrderBookAction.REPLACE) {
							orderBookAmount = data.findValue("amount").asText();
							if (orderBookAmount == null || orderBookAmount.isEmpty()) {
								throw new IllegalStateException("Received " + updateType + " with no amount in node " + node + " from payload " + payload);
							}
						}
						updates.add(new OrderBookUpdate(orderBookType, orderBookRate, orderBookAmount, action));
					}
				}
			});
		} catch (Exception e) {
			if (e instanceof ParsingPayloadException)
				throw (ParsingPayloadException)e;
			throw new ParsingPayloadException(e.getMessage(), e);
		}
		return updates;
	}

	public static void main(String[] args) throws InterruptedException {
		List<SubscriptionProperties> marketDataTopics = new ArrayList<SubscriptionProperties>();
		marketDataTopics.add(new SubscriptionProperties.Builder().currencyPair("BTCLTC").topic("BTC_LTC").build());
		//marketDataTopics.add(new SubscriptionProperties.Builder().currencyPair("BTCETH").topic("BTC_ETH").build());
		ServiceProperties properties = new ServiceProperties.Builder()
											.uri("wss://api.poloniex.com")
											.marketDataTopics(marketDataTopics)
											.transactionsPerSecond(6).build();
		OrderBookManager orderBookManager = new OrderBookManager();
		PoloniexService poloniex = new PoloniexService(properties, orderBookManager);
		poloniex.registerOrderBookListener(orderBook -> {
			System.out.println("DEBUG: orderBook: " + orderBook);
		});
		poloniex.connect();
		Thread.sleep(1000 * 60 * 2);
		poloniex.disconnect();
	}

}
