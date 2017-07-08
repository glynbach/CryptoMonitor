package com.kieral.cryptomon.service.exchange.yobit.payload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.rest.BaseOrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YobitOrderBookResponse extends BaseOrderBookResponse {

	private final Long createdTime = System.currentTimeMillis();
	private List<List<String>> bids;
	private List<List<String>> asks;

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
		return "YobitOrderBookResponse [createdTime=" + createdTime + ", bids=" + bids + ", asks=" + asks + "]";
	}

	@Override
	public long getSequence() {
		return OrderBookManager.AUTO_INCREMENT_SEQUENCE;
	}

}
