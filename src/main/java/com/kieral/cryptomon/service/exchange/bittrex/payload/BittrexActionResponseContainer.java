package com.kieral.cryptomon.service.exchange.bittrex.payload;

import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;

public class BittrexActionResponseContainer extends BittrexResponseContainer<BittrexActionResponse> implements PlaceOrderResponse, CancelOrderResponse {

	@Override
	public OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus) {
		if (clazz.isAssignableFrom(PlaceOrderResponse.class))
			return success ? OrderStatus.OPEN : OrderStatus.CANCELLED;
		if (clazz.isAssignableFrom(CancelOrderResponse.class))
			return success ? OrderStatus.CANCELLED : getMessage() != null && getMessage().contains("UUID_INVALID") ? OrderStatus.CANCELLED : OrderStatus.ERROR;
		return OrderStatus.ERROR;
	}

	@Override
	public String getOrderId() {
		return getResult() == null ? null : getResult().getUuid();
	}
	
	@Override
	public String getExchangeMessage() {
		return getMessage();
	}
	
}
