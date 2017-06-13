package com.kieral.cryptomon.service.exchange.gdax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxOrderBookResponse extends OrderBookResponse {

	private final Long createdTime = System.currentTimeMillis();
	private long sequence;
	private List<List<String>> bids;
	private List<List<String>> asks;

	public long getSequence() {
		return sequence;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	public List<List<String>> getBids() {
		return bids;
	}

	public void setBids(List<List<String>> bids) {
		this.bids = bids;
	}

	public List<List<String>> getAsks() {
		return asks;
	}

	public void setAsks(List<List<String>> asks) {
		this.asks = asks;
	}

	@Override
	public String toString() {
		return "GdaxOrderBookResponse [sequence=" + sequence + ", bids=" + bids + ", asks=" + asks + "]";
	}

	@Override
	public long getCreatedTime() {
		return createdTime;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public List<OrderBookResponseEntry> getBidEntries() {
		return getEntries(bids);
	}

	@Override
	public List<OrderBookResponseEntry> getAskEntries() {
		return getEntries(asks);
	}

	private List<OrderBookResponseEntry> getEntries(List<List<String>> entries) {
		List<OrderBookResponseEntry> rtn = new ArrayList<OrderBookResponseEntry>();
		if (entries != null) {
			entries.forEach(entrySet -> {
				if (entrySet.size() != 3)
					throw new IllegalStateException(String.format("Expect price,amount,numOrders but got %s", entrySet));
				rtn.add(new OrderBookResponseEntry(new BigDecimal(entrySet.get(0)), 
						new BigDecimal(entrySet.get(1)), Integer.parseInt(entrySet.get(2))));
			});
		}
		return rtn;
	}
	
}
