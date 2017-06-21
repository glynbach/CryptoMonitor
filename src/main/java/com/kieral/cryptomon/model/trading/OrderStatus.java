package com.kieral.cryptomon.model.trading;

import java.util.EnumSet;

public enum OrderStatus {

	PENDING,
	OPEN,
	PARTIALLY_FILLED,
	FILLED,
	CANCELLED,
	ERROR;
	
	public final static EnumSet<OrderStatus> OPEN_ORDER = EnumSet.of(OrderStatus.PENDING, OrderStatus.OPEN, OrderStatus.ERROR);
	public final static EnumSet<OrderStatus> CLOSED_ORDER = EnumSet.of(OrderStatus.FILLED, OrderStatus.CANCELLED);
	
}
