package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OpenOrderStatus;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;

public interface OrderResponse {
	
	boolean isSuccess();
	String getOrderId();
	BigDecimal getAmount();
	BigDecimal getAmountRemaining();
	BigDecimal getPrice();
	Side getSide();
	OrderStatus getOrderStatus();
	boolean isOpen();
	boolean isClosing();
	long getCreatedTime();
	long getClosedTime();
	OpenOrderStatus getOrderUpdateStatus(boolean isOpenOrderRequest, Order order);

}
