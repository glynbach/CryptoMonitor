package com.kieral.cryptomon.messaging;

import java.util.List;

public class ExchangeStatusMessage {

	private String name;
	private boolean connected;
	private boolean tradingLocked;
	private BalanceMessage balances;
	private List<OrderBookMessage> orderBooks;
	
	public ExchangeStatusMessage() {
	}
	
	public ExchangeStatusMessage(String name, boolean connected, boolean tradingLocked, BalanceMessage balances,
			List<OrderBookMessage> orderBooks) {
		this.name = name;
		this.connected = connected;
		this.tradingLocked = tradingLocked;
		this.balances = balances;
		this.orderBooks = orderBooks;
	}

	public ExchangeStatusMessage(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public boolean isTradingLocked() {
		return tradingLocked;
	}

	public void setTradingLocked(boolean tradingLocked) {
		this.tradingLocked = tradingLocked;
	}

	public BalanceMessage getBalances() {
		return balances;
	}

	public void setBalances(BalanceMessage balances) {
		this.balances = balances;
	}

	public List<OrderBookMessage> getOrderBooks() {
		return orderBooks;
	}

	public void setOrderBooks(List<OrderBookMessage> orderBooks) {
		this.orderBooks = orderBooks;
	}

	@Override
	public String toString() {
		return "ExchangeStatusMessage [name=" + name + ", connected=" + connected + ", tradingLocked=" + tradingLocked
				+ ", balances=" + balances + ", orderBooks=" + orderBooks + "]";
	}
	
}
