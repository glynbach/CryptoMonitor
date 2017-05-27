package com.kieral.cryptomon.model;

import java.util.Collections;
import java.util.List;

public class OrderBook {

	private final String market;
	private final String currencyPair;
	private List<IOrderBookEntry> bids = Collections.emptyList();
	private List<IOrderBookEntry> asks = Collections.emptyList();
	
	public OrderBook(String market, String currencyPair) {
		super();
		this.market = market;
		this.currencyPair = currencyPair;
	}
	
	public List<IOrderBookEntry> getBids() {
		return bids;
	}
	
	public void setBids(List<IOrderBookEntry> bids) {
		this.bids = bids;
	}
	
	public List<IOrderBookEntry> getAsks() {
		return asks;
	}
	
	public void setAsks(List<IOrderBookEntry> asks) {
		this.asks = asks;
	}
	
	public String getMarket() {
		return market;
	}
	
	public String getCurrencyPair() {
		return currencyPair;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((asks == null) ? 0 : asks.hashCode());
		result = prime * result + ((bids == null) ? 0 : bids.hashCode());
		result = prime * result + ((currencyPair == null) ? 0 : currencyPair.hashCode());
		result = prime * result + ((market == null) ? 0 : market.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrderBook other = (OrderBook) obj;
		if (asks == null) {
			if (other.asks != null)
				return false;
		} else if (!asks.equals(other.asks))
			return false;
		if (bids == null) {
			if (other.bids != null)
				return false;
		} else if (!bids.equals(other.bids))
			return false;
		if (currencyPair == null) {
			if (other.currencyPair != null)
				return false;
		} else if (!currencyPair.equals(other.currencyPair))
			return false;
		if (market == null) {
			if (other.market != null)
				return false;
		} else if (!market.equals(other.market))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderBook [market=" + market + ", currencyPair=" + currencyPair + ", bids=" + bids + ", asks=" + asks
				+ "]";
	}
	
}
