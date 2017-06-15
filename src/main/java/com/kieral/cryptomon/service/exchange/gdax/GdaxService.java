package com.kieral.cryptomon.service.exchange.gdax;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
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
	protected Class<? extends PlaceOrderResponse> getPlaceOrderQuery(Side side, CurrencyPair currencyPair,
			BigDecimal price, BigDecimal amount) {
		return GdaxActionResponse.class;
	}

	@Override
	protected Class<? extends CancelOrderResponse> getCancelOrderQuery(String orderId) {
		return GdaxActionResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse> getOpenOrdersQuery(CurrencyPair currencyPair) {
		return GdaxOrdersResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse> getOrderHistoryQuery(CurrencyPair currencyPair) {
		return GdaxFillsResponse.class;
	}

	@Override
	protected Class<? extends OrderResponse> getOrderQuery(String orderId) {
		return GdaxOrderResponse.class;
	}
	
}
