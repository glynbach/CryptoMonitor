package com.kieral.cryptomon.service.poloniex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.IOrderBookEntry;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookAction;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.ServiceProperties;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.liquidity.AbstractLiquidityProvider;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.liquidity.SubscriptionProperties;
import com.kieral.cryptomon.service.util.HttpUtils;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;
import com.kieral.cryptomon.streaming.StreamingProperties;
import com.kieral.cryptomon.streaming.StreamingProvider;
import com.kieral.cryptomon.streaming.wamp.WampStreamingProvider;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class PoloniexService extends AbstractLiquidityProvider {

	private static final String POLONIEX = "Poloniex";
	private static final String SNAPSHOT_URL = "https://poloniex.com/public?command=returnOrderBook&currencyPair=%s&depth=10";

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
	public String getName() {
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
		logger.info("Received payload: {}", payload);
		try {
			json.elements().forEachRemaining(node -> {
				JsonNode type = node.findValue("type");
				if (type != null && type.isTextual()) {
					String updateType = type.asText();
					if (updateType.equalsIgnoreCase("orderBookModify") || updateType.equalsIgnoreCase("orderBookRemove")) {
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

	@Override
	protected OrderBook subscribeOrderbookSnapshot(String topic, String currencyPair) throws JsonProcessingException, IOException {
		String url = String.format(SNAPSHOT_URL, topic);
		logger.info("Requesting orderbook snapshot from {}", url);
		JsonNode json = HttpUtils.getResponseAsJson(url);
		logger.info("Orderbook snapshot response {}", json);
		LoggingUtils.logRawData(String.format("%s: snapshot: %s", getName(), json));
		List<OrderBookUpdate> askUpdates = new ArrayList<OrderBookUpdate>();
		List<OrderBookUpdate> bidUpdates = new ArrayList<OrderBookUpdate>();
		JsonNode asks = json.get("asks");
		if (asks != null) {
			asks.elements().forEachRemaining(askNode -> {
				askUpdates.add(new OrderBookUpdate("ask", askNode.get(0).asText(), askNode.get(1).asText(), OrderBookAction.REPLACE));
			});
		}
		JsonNode bids = json.get("bids");
		if (bids != null) {
			bids.elements().forEachRemaining(bidNode -> {
				askUpdates.add(new OrderBookUpdate("bid", bidNode.get(0).asText(), bidNode.get(1).asText(), OrderBookAction.REPLACE));
			});
		}
		String seq = json.findValue("seq").asText();
		//String isFrozen = json.findValue("isFrozen").asText();
		orderBookManager.updateOrderBook(POLONIEX, currencyPair, askUpdates, true);
		OrderBook orderBookSnapshot = orderBookManager.updateOrderBook(POLONIEX, currencyPair, bidUpdates, false);
		orderBookSnapshot.setSnapshotSequence(Long.parseLong(seq));
		//TODO: add frozen concept
		return orderBookSnapshot;
	}

	public static void main(String[] args) throws InterruptedException {
		List<SubscriptionProperties> marketDataTopics = new ArrayList<SubscriptionProperties>();
		marketDataTopics.add(new SubscriptionProperties.Builder().currencyPair("BTCLTC").topic("BTC_LTC").build());
		//marketDataTopics.add(new SubscriptionProperties.Builder().currencyPair("BTCETH").topic("BTC_ETH").build());
		ServiceProperties properties = new ServiceProperties.Builder()
											.uri("wss://api.poloniex.com")
											.marketDataTopics(marketDataTopics)
											.transactionsPerSecond(6)
											.sipValidationOnEmptyPayloads(true)
											.requiresSnapshot(true)
											.build();
		OrderBookManager orderBookManager = new OrderBookManager();
		PoloniexService poloniex = new PoloniexService(properties, orderBookManager);
		poloniex.registerOrderBookListener(orderBook -> {
			print(orderBook);
		});
		poloniex.connect();
		//Thread.sleep(5000);
		Thread.sleep(1000 * 60 * 1);
		poloniex.disconnect();
	}

	private static void print(OrderBook orderBook) {
		System.out.println(orderBook.getMarket() + " - " + orderBook.getCurrencyPair());
		System.out.println("Asks\t\t\tBids\t\t\t");
		System.out.println("Price\tAmount\tPrice\tAmount\t");
		for (int i=0; i<10; i++) {
			List<IOrderBookEntry> asks = orderBook.getAsks();
			List<IOrderBookEntry> bids = orderBook.getBids();
			StringBuffer line = new StringBuffer();
			if (asks.size() > (i+1)) 
				line.append(asks.get(i).getPrice().toPlainString()+"\t"+asks.get(i).getAmount().toPlainString()+"\t");
			else 
				line.append("\t\t\t");
			if (bids.size() > (i+1)) 
				line.append(bids.get(i).getPrice().toPlainString()+"\t"+bids.get(i).getAmount().toPlainString()+"\t");
			else 
				line.append("\t\t\t");
			System.out.println(line.toString());
		}
	}
}
