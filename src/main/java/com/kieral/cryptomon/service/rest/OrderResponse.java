package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OrderStatus;

public interface OrderResponse {
	
	boolean isSuccess();
	String getOrderId();
	BigDecimal getQuantity();
	BigDecimal getQuantityRemaining();
	BigDecimal getPrice();
	Side getSide();
	OrderStatus getOrderStatus();
	boolean isOpen();
	boolean isClosing();
	long getCreatedTime();
	long getClosedTime();

}
