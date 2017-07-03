package com.kieral.cryptomon.model.trading;

import java.util.EnumSet;

public enum OrderStatus {

	INITIALISED,
	SENDING,
	SENT,
	OPEN,
	PARTIALLY_FILLED,
	FILLED,
	CANCELLED,
	ERROR;
	
	public final static EnumSet<OrderStatus> OPEN_ORDER = EnumSet.of(OrderStatus.INITIALISED, OrderStatus.SENDING, OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED, OrderStatus.ERROR);
	public final static EnumSet<OrderStatus> CLOSED_ORDER = EnumSet.of(OrderStatus.FILLED, OrderStatus.CANCELLED);
	
}
