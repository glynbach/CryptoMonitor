package com.kieral.cryptomon.service.rest;

import com.kieral.cryptomon.model.trading.OrderStatus;

public interface CancelOrderResponse {

	OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus);

	String getExchangeMessage();

}
