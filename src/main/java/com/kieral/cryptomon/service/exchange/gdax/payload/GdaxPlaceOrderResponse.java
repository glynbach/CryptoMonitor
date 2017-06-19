package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxPlaceOrderResponse implements PlaceOrderResponse {

	String id;
	String status;
	String message;
	String[] entries;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String[] getEntries() {
		return entries;
	}

	public void setEntries(String[] entries) {
		this.entries = entries;
	}

	@Override
	public String toString() {
		return "GdaxActionResponse [id=" + id + ", status=" + status + ", message=" + message + ", entries="
				+ Arrays.toString(entries) + "]";
	}

	@Override
	public OrderStatus getOrderStatus(Class<?> clazz, OrderStatus currentStatus) {
		return !CommonUtils.isEmpty(id) && !CommonUtils.isEmpty(status) &&
				(status.toUpperCase().equals("PENDING") || status.toUpperCase().equals("OPEN") || status.toUpperCase().equals("ACTIVE")) 
				? OrderStatus.OPEN : OrderStatus.CANCELLED;
	}

	@Override
	public String getExchangeMessage() {
		return message;
	}

	@Override
	public String getOrderId() {
		return id;
	}

}
