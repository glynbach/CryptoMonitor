package com.kieral.cryptomon.service.exchange.bittrex;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexAccountsResponse;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexActionResponse;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexOrderBookResponse;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexOrderResponseContainer;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexOrdersResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class BittrexService extends BaseExchangeService {

	public BittrexService(BittrexServiceConfig serviceProperties, ServiceSecurityModule securityModule) {
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
		throw new IllegalStateException("Bittrex has no streaming topics");
	}
	
	@Override
	protected List<OrderBookUpdate> parsePayload(StreamingPayload payload) throws ParsingPayloadException {
		throw new IllegalStateException("Streaming payloads not supported");
	}

	@Override
	protected void unsubscribeMarketDataTopics() {
	}

	@Override
	protected Class<? extends OrderBookResponse> getOrderBookResponseClazz() {
		return BittrexOrderBookResponse.class;
	}

	@Override
	protected Class<? extends AccountsResponse> getAccountsResponseClazz() {
		return BittrexAccountsResponse.class;
	}

	@Override
	protected Class<? extends PlaceOrderResponse> getPlaceOrderQuery(Side side, CurrencyPair currencyPair,
			BigDecimal price, BigDecimal amount) {
		return BittrexActionResponse.class;
	}

	@Override
	protected Class<? extends CancelOrderResponse> getCancelOrderQuery(String orderId) {
		return BittrexActionResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse> getOpenOrdersQuery(CurrencyPair currencyPair) {
		return BittrexOrdersResponse.class;
	}

	@Override
	protected Class<? extends OrdersResponse> getOrderHistoryQuery(CurrencyPair currencyPair) {
		return BittrexOrdersResponse.class;
	}

	@Override
	protected Class<? extends OrderResponse> getOrderQuery(String orderId) {
		return BittrexOrderResponseContainer.class;
	}

}
