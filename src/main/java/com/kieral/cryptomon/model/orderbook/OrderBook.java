package com.kieral.cryptomon.model.orderbook;

import java.util.Collections;
import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;

public class OrderBook {

	private final String market;
	private final CurrencyPair currencyPair;
	private long snapshotSequence;
	private long snapshotReceived;
	private List<IOrderBookEntry> bids = Collections.emptyList();
	private List<IOrderBookEntry> asks = Collections.emptyList();
	
	public OrderBook(String market, CurrencyPair currencyPair, long sequence, long createdTime) {
		this.market = market;
		this.currencyPair = currencyPair;
		this.snapshotSequence = sequence;
		this.snapshotReceived = createdTime;
	}
	
	public long getSnapshotSequence() {
		return snapshotSequence;
	}

	public void setSnapshotSequence(long snapshotSequence) {
		this.snapshotSequence = snapshotSequence;
	}

	public long getSnapshotReceived() {
		return snapshotReceived;
	}

	public void setSnapshotReceived(long snapshotReceived) {
		this.snapshotReceived = snapshotReceived;
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
	
	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currencyPair == null) ? 0 : currencyPair.hashCode());
		result = prime * result + ((market == null) ? 0 : market.hashCode());
		result = prime * result + (int) (snapshotReceived ^ (snapshotReceived >>> 32));
		result = prime * result + (int) (snapshotSequence ^ (snapshotSequence >>> 32));
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
		if (snapshotReceived != other.snapshotReceived)
			return false;
		if (snapshotSequence != other.snapshotSequence)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderBook [market=" + market + ", currencyPair=" + currencyPair + ", snapshotSequence="
				+ snapshotSequence + ", snapshotReceived=" + snapshotReceived + ", bids=" + bids + ", asks=" + asks
				+ "]";
	}
	
}
