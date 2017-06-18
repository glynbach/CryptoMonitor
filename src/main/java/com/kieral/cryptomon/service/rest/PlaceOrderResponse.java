package com.kieral.cryptomon.service.rest;

import com.kieral.cryptomon.model.trading.OrderStatus;

public interface PlaceOrderResponse {

	OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus);

	String getOrderId();

	String getExchangeMessage();

}
