package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

public class OrderBookResponseEntry {

	private BigDecimal price;
	private BigDecimal amount;
	private int numOrders;
	
	public OrderBookResponseEntry() {
	}
	
	public OrderBookResponseEntry(BigDecimal price, BigDecimal amount, int numOrders) {
		this.price = price;
		this.amount = amount;
		this.numOrders = numOrders;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public int getNumOrders() {
		return numOrders;
	}

	public void setNumOrders(int numOrders) {
		this.numOrders = numOrders;
	}

	@Override
	public String toString() {
		return "OrderBookResponseEntry [price=" + price + ", amount=" + amount + ", numOrders=" + numOrders + "]";
	}
	
}
