package com.kieral.cryptomon.model.trading;

import java.math.BigDecimal;

public class OpenOrderStatus {

	private final Order order;
	private final OrderStatus newStatus;
	private final BigDecimal amountRemaining;
	
	public OpenOrderStatus(Order order, OrderStatus newStatus, BigDecimal amountRemaining) {
		this.order = order;
		this.newStatus = newStatus;
		this.amountRemaining = amountRemaining;
	}

	public Order getOrder() {
		return order;
	}

	public OrderStatus getNewStatus() {
		return newStatus;
	}

	public BigDecimal getAmountRemaining() {
		return amountRemaining;
	}

	@Override
	public String toString() {
		return "OpenOrderStatus [order=" + order + ", newStatus=" + newStatus + ", amountRemaining=" + amountRemaining + "]";
	}
	
}
