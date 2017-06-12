package com.kieral.cryptomon.service.exchange.bittrex;

import java.util.List;

import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.exchange.BaseExchangeService;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.streaming.ParsingPayloadException;
import com.kieral.cryptomon.streaming.StreamingPayload;

public class BittrexService extends BaseExchangeService {

	public BittrexService(BittrexServiceConfig serviceProperties) {
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

}
