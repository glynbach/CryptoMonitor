package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxCancelOrderResponse extends ArrayList<String> implements CancelOrderResponse {

	private static final long serialVersionUID = 7379381553477688221L;
	
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus) {
		return CommonUtils.isEmpty(message) ? OrderStatus.CANCELLED : currentStatus;
	}

	@Override
	public String getExchangeMessage() {
		return message == null ? this.subList(0,  this.size()).toString() : message;
	}

	@Override
	public String toString() {
		return "GdaxCancelOrderResponse [message=" + message + ", entries=" + this.subList(0,  this.size()) + "]";
	}
	
}
