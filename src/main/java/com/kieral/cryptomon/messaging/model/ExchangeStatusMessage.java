package com.kieral.cryptomon.messaging.model;

import java.util.List;

public class ExchangeStatusMessage {

	private String name;
	private boolean connected;
	private boolean tradingEnabled;
	private BalanceMessage balances;
	private List<OrderBookMessage> orderBooks;
	private List<OrderMessage> openOrders;
	private List<OrderMessage> closedOrders;
	
	public ExchangeStatusMessage() {
	}
	
	public ExchangeStatusMessage(String name, boolean connected, boolean tradingEnabled, BalanceMessage balances,
			List<OrderBookMessage> orderBooks, List<OrderMessage> openOrders, List<OrderMessage> closedOrders) {
		this.name = name;
		this.connected = connected;
		this.tradingEnabled = tradingEnabled;
		this.balances = balances;
		this.orderBooks = orderBooks;
		this.openOrders = openOrders;
		this.closedOrders = closedOrders;
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

	public boolean isTradingEnabled() {
		return tradingEnabled;
	}

	public void setTradingEnabled(boolean tradingEnabled) {
		this.tradingEnabled = tradingEnabled;
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

	public List<OrderMessage> getOpenOrders() {
		return openOrders;
	}

	public void setOpenOrders(List<OrderMessage> openOrders) {
		this.openOrders = openOrders;
	}

	public List<OrderMessage> getClosedOrders() {
		return closedOrders;
	}

	public void setClosedOrders(List<OrderMessage> closedOrders) {
		this.closedOrders = closedOrders;
	}

	@Override
	public String toString() {
		return "ExchangeStatusMessage [name=" + name + ", connected=" + connected + ", tradingEnabled=" + tradingEnabled
				+ ", balances=" + balances + ", orderBooks=" + orderBooks + ", openOrders=" + openOrders
				+ ", closedOrders=" + closedOrders + "]";
	}
	
}
