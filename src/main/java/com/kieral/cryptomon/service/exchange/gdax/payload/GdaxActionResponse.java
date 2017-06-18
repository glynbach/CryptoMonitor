package com.kieral.cryptomon.service.exchange.gdax.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxActionResponse implements PlaceOrderResponse, CancelOrderResponse {

	String orderId;
	String message;
	
	public String getOrderId() {
		return orderId;
	}
	
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "GdaxActionResponse [orderId=" + orderId + ", message=" + message + "]";
	}

	@Override
	public OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus) {
		if (clazz.isAssignableFrom(PlaceOrderResponse.class))
			return !CommonUtils.isEmpty(orderId) ? OrderStatus.OPEN : OrderStatus.CANCELLED;
		if (clazz.isAssignableFrom(CancelOrderResponse.class))
			return CommonUtils.isEmpty(message) ? OrderStatus.CANCELLED : currentStatus;
		return OrderStatus.ERROR;
	}

	
	@Override
	public String getExchangeMessage() {
		return message;
	}

}
