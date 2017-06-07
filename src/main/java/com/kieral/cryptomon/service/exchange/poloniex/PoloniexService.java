package com.kieral.cryptomon.service.exchange.poloniex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookAction;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.liquidity.BaseService;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.HttpUtils;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;
import com.kieral.cryptomon.streaming.StreamingProperties;
import com.kieral.cryptomon.streaming.StreamingProvider;
import com.kieral.cryptomon.streaming.wamp.WampStreamingProvider;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class PoloniexService extends BaseService {

	private final StreamingProvider streamingProvider = new WampStreamingProvider();
	private final StreamingProperties streamingProperties;

	private AtomicLong lastSequence = new AtomicLong(-1);
	
	public PoloniexService(PoloniexServiceConfig serviceProperties, OrderBookManager orderBookManager) {
		super(serviceProperties, orderBookManager);
		if (serviceProperties == null)
			throw new IllegalArgumentException("serviceProperties can not be null");
		streamingProperties = new StreamingProperties.Builder()
				.uri(serviceProperties.getPushApi())
				.realm("realm1")
				.build();
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
		List<CurrencyPair> currencyPairs = serviceProperties.getPairs();
		if (currencyPairs != null)
			currencyPairs.forEach(pair -> {
				Observable<StreamingPayload> result = streamingProvider.subscribe(pair);
				result.subscribe(streamingPayload -> {
					onPayloadUpdate(streamingPayload);
				});
			});
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		List<OrderBookUpdate> updates = new ArrayList<OrderBookUpdate>();
		JsonNode json = payload.getJson();
		if (logger.isDebugEnabled())
			logger.debug("Received payload: {}", payload);
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
	protected OrderBook getOrderBookSnapshot(CurrencyPair currencyPair) throws JsonProcessingException, IOException {
		String url = serviceProperties.getOrderBookSnapshotQuery(currencyPair.getTopic());
		logger.info("Requesting orderbook snapshot from {}", url);
		JsonNode json = HttpUtils.getResponseAsJson(url);
		logger.info("Orderbook snapshot response {}", json);
		LoggingUtils.logRawData(String.format("%s: snapshot: %s", getName(), json));
		return processOrderBookNode(json, currencyPair);
	}

	@Override
	protected List<OrderBook> getOrderBookSnapshots(List<CurrencyPair> pairs)
			throws JsonProcessingException, IOException {
		if (pairs == null)
			return null;
		List<OrderBook> orderBooks = new ArrayList<OrderBook>();
		String url = serviceProperties.getOrderBookSnapshotQuery("all");
		logger.info("Requesting orderbook snapshot from {}", url);
		JsonNode json = HttpUtils.getResponseAsJson(url);
		logger.info("Orderbook snapshot response {}", json);
		LoggingUtils.logRawData(String.format("%s: snapshot: %s", getName(), json));
		pairs.forEach(pair -> {
			JsonNode pairNode = json.findValue(pair.getTopic());
			if (pairNode != null && pairNode.size() > 0)
				orderBooks.add(processOrderBookNode(pairNode, pair));
		});
		return orderBooks;
	}

	private OrderBook processOrderBookNode(JsonNode json, CurrencyPair currencyPair) {
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
				bidUpdates.add(new OrderBookUpdate("bid", bidNode.get(0).asText(), bidNode.get(1).asText(), OrderBookAction.REPLACE));
			});
		}
		String seq = json.findValue("seq").asText();
		//String isFrozen = json.findValue("isFrozen").asText();
		orderBookManager.updateOrderBook(getName(), currencyPair, askUpdates, true);
		OrderBook orderBookSnapshot = orderBookManager.updateOrderBook(getName(), currencyPair, bidUpdates, false);
		orderBookSnapshot.setSnapshotSequence(Long.parseLong(seq));
		//TODO: add frozen concept
		return orderBookSnapshot;
	}
	
	@Override
	protected void unsubscribeMarketDataTopics() {
		// TODO Auto-generated method stub
		
	}

}
