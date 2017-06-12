package com.kieral.cryptomon.service.exchange.gdax;

import java.util.List;

import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class GdaxService extends BaseExchangeService {

	public GdaxService(GdaxServiceConfig serviceProperties) {
		super(serviceProperties);
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

}
