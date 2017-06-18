package com.kieral.cryptomon.service.exchange.gdax;

import java.util.EnumSet;
import java.util.List;

import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxAccountsResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxActionResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxFillsResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxOrderBookResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxOrderResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxOrdersResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class GdaxService extends BaseExchangeService {

	public GdaxService(GdaxServiceConfig serviceProperties, ServiceSecurityModule securityModule) {
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
		throw new IllegalStateException("subscribeMarketDataTopics not yet implemented");
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		throw new IllegalStateException("parsePayload not yet implemented");
	}

	@Override
	protected void unsubscribeMarketDataTopics() {
	}

	@Override
	protected Class<? extends OrderBookResponse> getOrderBookResponseClazz() {
		return GdaxOrderBookResponse.class;
	}

	@Override
	protected Class<? extends AccountsResponse> getAccountsResponseClazz() {
		return GdaxAccountsResponse.class;
	}

	@Override
	protected Class<? extends PlaceOrderResponse> getPlaceOrderResponseClazz() {
		return GdaxActionResponse.class;
	}

	@Override
	protected Class<? extends CancelOrderResponse> getCancelOrderResponseClazz() {
		return GdaxActionResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOpenOrdersResponseClazz() {
		return GdaxOrdersResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse<? extends OrderResponse>> getOrderHistoryResponseClazz() {
		return GdaxFillsResponse.class;
	}

	@Override
	protected Class<? extends OrderResponse> getOrderResponseClazz() {
		return GdaxOrderResponse.class;
	}
	
	@Override
	protected EnumSet<OrderCheckingStrategy> getOrderCheckingStrategies() {
		return EnumSet.of(OrderCheckingStrategy.CHECK_BY_INDIVIDUAL, OrderCheckingStrategy.CHECK_BY_ALL);
	}

}
