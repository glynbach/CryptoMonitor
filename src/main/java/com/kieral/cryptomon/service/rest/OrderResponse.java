package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;

public interface OrderResponse {
	
	public enum RequestNature {
		CLOSED_ORDER_RESPONSE,
		OPEN_ORDER_RESPONSE,
		UNKNOWN_ORDER_RESPONSES
	}
	
	boolean isSuccess();
	String getOrderId();
	BigDecimal getAmount();
	BigDecimal getPrice();
	Side getSide();
	boolean isOpen();
	long getCreatedTime();
	long getClosedTime();
	TradesResponse getTradeResponses();
	
	OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder);

}
