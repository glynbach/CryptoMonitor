package com.kieral.cryptomon.messaging;

import java.util.ArrayList;
import java.util.List;

import com.kieral.cryptomon.model.OrderBook;

public class OrderBookMessage {

	private String market;
	private String currencyPair;
	private List<String[]> bids;
	private List<String[]> asks;
	
	public OrderBookMessage() {
	}

	public OrderBookMessage(String market, String currencyPair, List<String[]> bids, List<String[]> asks) {
		super();
		this.market = market;
		this.currencyPair = currencyPair;
		this.bids = bids;
		this.asks = asks;
	}

	public OrderBookMessage(OrderBook orderBook) {
		if (orderBook != null) {
			this.market = orderBook.getMarket();
			this.currencyPair = orderBook.getCurrencyPair();
			bids = new ArrayList<String[]>();
			asks = new ArrayList<String[]>();
			orderBook.getBids().forEach(entry -> {
				bids.add(new String[] {entry.getPrice().toPlainString(),
										entry.getAmount().toPlainString()});
			});
			orderBook.getAsks().forEach(entry -> {
				asks.add(new String[] {entry.getPrice().toPlainString(),
										entry.getAmount().toPlainString()});
			});
		}
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(String currencyPair) {
		this.currencyPair = currencyPair;
	}

	public List<String[]> getBids() {
		return bids;
	}

	public void setBids(List<String[]> bids) {
		this.bids = bids;
	}

	public List<String[]> getAsks() {
		return asks;
	}

	public void setAsks(List<String[]> asks) {
		this.asks = asks;
	}
	
}
