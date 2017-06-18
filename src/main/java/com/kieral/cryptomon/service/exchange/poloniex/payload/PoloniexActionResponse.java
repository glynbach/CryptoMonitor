package com.kieral.cryptomon.service.exchange.poloniex.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexActionResponse implements PlaceOrderResponse, CancelOrderResponse {

	private int success;
	private String orderNumber;
	
	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	@Override
	public OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus) {
		if (clazz.isAssignableFrom(PlaceOrderResponse.class))
			return !CommonUtils.isEmpty(orderNumber) ? OrderStatus.OPEN : OrderStatus.CANCELLED;
		if (clazz.isAssignableFrom(CancelOrderResponse.class))
			return success == 1 ? OrderStatus.CANCELLED : currentStatus;
		return OrderStatus.ERROR;
	}

	@Override
	public String getOrderId() {
		return orderNumber;
	}

	@Override
	public String getExchangeMessage() {
		return null;
	}

	@Override
	public String toString() {
		return "PoloniexActionResponse [success=" + success + ", orderNumber=" + orderNumber + "]";
	}
	
}
