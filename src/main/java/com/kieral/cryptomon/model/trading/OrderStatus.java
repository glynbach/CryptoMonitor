package com.kieral.cryptomon.model.trading;

import java.util.EnumSet;

public enum OrderStatus {

	PENDING,
	OPEN,
	PARTIALLY_FILLED,
	FILLED,
	CANCELLED,
	SETTLED,
	ERROR;
	
	public final static EnumSet<OrderStatus> OPEN_ORDER = EnumSet.of(OrderStatus.PENDING, OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED, OrderStatus.ERROR);
	public final static EnumSet<OrderStatus> CLOSED_ORDER = EnumSet.of(OrderStatus.FILLED, OrderStatus.CANCELLED, OrderStatus.SETTLED);
	
}
