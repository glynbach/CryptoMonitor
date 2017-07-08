package com.kieral.cryptomon.service.exchange.bitfinex;

import java.util.EnumSet;
import java.util.List;

import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;
import com.kieral.cryptomon.service.exchange.bitfine.payload.BitfinexAccountsResponse;
import com.kieral.cryptomon.service.exchange.bitfine.payload.BitfinexOrderBookResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class BitfinexService extends BaseExchangeService {

	public BitfinexService(BitfinexServiceConfig serviceProperties, ServiceSecurityModule securityModule) {
		super(serviceProperties, securityModule);
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
		throw new IllegalStateException("Bitfinex streaming topics not implemented");
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		throw new IllegalStateException("Streaming payloads not implemented");
	}

	@Override
	protected void unsubscribeMarketDataTopics() {
	}

	@Override
	protected Class<? extends OrderBookResponse> getOrderBookResponseClazz() {
		return BitfinexOrderBookResponse.class;
	}

	@Override
	protected Class<? extends AccountsResponse> getAccountsResponseClazz() {
		return BitfinexAccountsResponse.class;
	}

	@Override
	protected Class<? extends PlaceOrderResponse> getPlaceOrderResponseClazz() {
		return null;
	}

	@Override
	protected Class<? extends CancelOrderResponse> getCancelOrderResponseClazz() {
		return null;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOpenOrdersResponseClazz() {
		return null;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOrderHistoryResponseClazz() {
		return null;
	}

	@Override
	protected Class<? extends OrderResponse> getOrderResponseClazz() {
		return null;
	}

	@Override
	protected EnumSet<OrderCheckingStrategy> getOrderCheckingStrategies() {
		return EnumSet.of(OrderCheckingStrategy.CHECK_BY_INDIVIDUAL, OrderCheckingStrategy.CHECK_BY_OPEN_ORDERS, OrderCheckingStrategy.CHECK_BY_TRADE_HISTORY);
	}

}
