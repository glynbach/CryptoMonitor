package com.kieral.cryptomon.service.exchange.poloniex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBookAction;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexAccountsResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexActionResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderBookResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderHistoryResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderTradesResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOpenOrdersResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;
import com.kieral.cryptomon.streaming.StreamingProperties;
import com.kieral.cryptomon.streaming.StreamingProvider;
import com.kieral.cryptomon.streaming.wamp.WampStreamingProvider;

import io.reactivex.Completable;
import io.reactivex.Observable;

public class PoloniexService extends BaseExchangeService {

	private final StreamingProvider streamingProvider = new WampStreamingProvider();
	private final StreamingProperties streamingProperties;

	private AtomicLong lastSequence = new AtomicLong(-1);
	
	public PoloniexService(PoloniexServiceConfig serviceProperties, ServiceSecurityModule securityModule) {
		super(serviceProperties, securityModule);
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
						updates.add(new OrderBookUpdate(orderBookTypeFrom(orderBookType), 
								new BigDecimal(orderBookRate), orderBookAmount == null ? null : new BigDecimal(orderBookAmount), action));
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

	private Side orderBookTypeFrom(String orderBookType) {
		if ("BID".equalsIgnoreCase(orderBookType))
			return Side.BID;
		if ("ASK".equalsIgnoreCase(orderBookType))
			return Side.ASK;
		throw new IllegalArgumentException(String.format("Unknown orderBookType %s", orderBookType));
	}
	
	@Override
	protected Class<? extends OrderBookResponse> getOrderBookResponseClazz() {
		return PoloniexOrderBookResponse.class;
	}
	
	@Override
	protected void unsubscribeMarketDataTopics() {
	}

	@Override
	protected Class<? extends AccountsResponse> getAccountsResponseClazz() {
		return PoloniexAccountsResponse.class;
	}

	@Override
	protected Class<? extends PlaceOrderResponse> getPlaceOrderResponseClazz() {
		return PoloniexActionResponse.class;
	}

	@Override
	protected Class<? extends CancelOrderResponse> getCancelOrderResponseClazz() {
		return PoloniexActionResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOpenOrdersResponseClazz() {
		return PoloniexOpenOrdersResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOrderHistoryResponseClazz() {
		return PoloniexOrderHistoryResponse.class;
	}

	@Override
	protected Class<? extends OrderResponse> getOrderResponseClazz() {
		return PoloniexOrderTradesResponse.class;
	}

	@Override
	protected EnumSet<OrderCheckingStrategy> getOrderCheckingStrategies() {
		return EnumSet.of(OrderCheckingStrategy.CHECK_BY_OPEN_ORDERS, OrderCheckingStrategy.CHECK_BY_TRADE_HISTORY);
	}

}
