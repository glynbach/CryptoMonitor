package com.kieral.cryptomon.service.exchange.poloniex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderBookResponse extends OrderBookResponse {

	private final Long createdTime = System.currentTimeMillis();
	private long seq;
	private String isFrozen;
	private List<List<String>> bids;
	private List<List<String>> asks;

	public long getSeq() {
		return seq;
	}

	public void setSeq(long seq) {
		this.seq = seq;
	}

	public String getIsFrozen() {
		return isFrozen;
	}

	public void setIsFrozen(String isFrozen) {
		this.isFrozen = isFrozen;
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
	public long getCreatedTime() {
		return createdTime;
	}

	@Override
	public long getSequence() {
		return seq;
	}

	@Override
	public boolean isValid() {
		return "0".equals(isFrozen);
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
				if (entrySet.size() != 2)
					throw new IllegalStateException(String.format("Expect price,amount but got %s", entrySet));
				rtn.add(new OrderBookResponseEntry(new BigDecimal(entrySet.get(0)), 
						new BigDecimal(entrySet.get(1)), 0));
			});
		}
		return rtn;
	}

	@Override
	public String toString() {
		return "PoloniexOrderBookResponse [createdTime=" + createdTime + ", seq=" + seq + ", isFrozen=" + isFrozen
				+ ", bids=" + bids + ", asks=" + asks + "]";
	}
	
}
