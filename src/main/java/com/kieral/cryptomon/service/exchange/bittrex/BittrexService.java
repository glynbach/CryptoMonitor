package com.kieral.cryptomon.service.exchange.bittrex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.OrderBookAction;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.service.liquidity.BaseService;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.HttpUtils;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class BittrexService extends BaseService {

	public BittrexService(BittrexServiceConfig serviceProperties, OrderBookManager orderBookManager) {
		super(serviceProperties, orderBookManager);
		if (serviceProperties == null)
			throw new IllegalArgumentException("serviceProperties can not be null");
	}
	
	@Override
	protected boolean doConnect() {
		return true;
	}

	@Override
	protected boolean doDisconnect() {
		return true;
	}

	@Override
	protected void subscribeMarketDataTopics() {
		throw new IllegalStateException("Bittrex has no streaming topics");
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		throw new IllegalStateException("Streaming payloads not supported");
	}

	@Override
	protected OrderBook getOrderBookSnapshot(CurrencyPair currencyPair) throws JsonProcessingException, IOException {
		String url = serviceProperties.getOrderBookSnapshotQuery(currencyPair.getTopic());
		logger.info("Requesting orderbook snapshot from {}", url);
		JsonNode json = HttpUtils.getResponseAsJson(url);
		logger.info("Orderbook snapshot response {}", json);
		LoggingUtils.logRawData(String.format("%s: snapshot: %s", getName(), json));
		if (json.get("success").asBoolean()) {
			return processOrderBookNode(json.get("result"), currencyPair);
		} else {
			logger.warn("Order book retrieval was not a success");
			return null;
		}
	}

	@Override
	protected List<OrderBook> getOrderBookSnapshots(List<CurrencyPair> pairs)
			throws JsonProcessingException, IOException {
		if (pairs == null)
			return null;
		List<OrderBook> orderBooks = new ArrayList<OrderBook>();
		for (CurrencyPair pair : pairs) {
			OrderBook orderBook = getOrderBookSnapshot(pair);
			if (orderBook != null)
				orderBooks.add(orderBook);
		}
		return orderBooks;
	}

	private OrderBook processOrderBookNode(JsonNode json, CurrencyPair currencyPair) throws JsonProcessingException, IOException {
		List<OrderBookUpdate> askUpdates = new ArrayList<OrderBookUpdate>();
		List<OrderBookUpdate> bidUpdates = new ArrayList<OrderBookUpdate>();
		JsonNode asks = json.get("sell");
		if (asks != null) {
			asks.elements().forEachRemaining(askNode -> {
				askUpdates.add(new OrderBookUpdate("ask", askNode.get("Rate").asText(), askNode.get("Quantity").asText(), OrderBookAction.REPLACE));
			});
		}
		JsonNode bids = json.get("buy");
		if (bids != null) {
			bids.elements().forEachRemaining(bidNode -> {
				bidUpdates.add(new OrderBookUpdate("bid", bidNode.get("Rate").asText(), bidNode.get("Quantity").asText(), OrderBookAction.REPLACE));
			});
		}
		orderBookManager.updateOrderBook(getName(), currencyPair, askUpdates, true);
		OrderBook orderBookSnapshot = orderBookManager.updateOrderBook(getName(), currencyPair, bidUpdates, false);
		return orderBookSnapshot;
	}
	
	@Override
	protected void unsubscribeMarketDataTopics() {
	}

}
