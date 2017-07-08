package com.kieral.cryptomon.service.exchange.bitfine.payload;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.rest.BaseOrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitfinexOrderBookResponse extends BaseOrderBookResponse {

	List<BitfinexOrderBookEntryResponse> bids;
	List<BitfinexOrderBookEntryResponse> asks;
	
	public List<BitfinexOrderBookEntryResponse> getBids() {
		return bids;
	}

	public void setBids(List<BitfinexOrderBookEntryResponse> bids) {
		this.bids = bids;
	}

	public List<BitfinexOrderBookEntryResponse> getAsks() {
		return asks;
	}

	public void setAsks(List<BitfinexOrderBookEntryResponse> asks) {
		this.asks = asks;
	}

	@Override
	public long getSequence() {
		return OrderBookManager.AUTO_INCREMENT_SEQUENCE;
	}

	@Override
	public long getCreatedTime() {
		return System.currentTimeMillis();
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public List<? extends OrderBookResponseEntry> getBidEntries() {
		return bids;
	}

	@Override
	public List<? extends OrderBookResponseEntry> getAskEntries() {
		return asks;
	}

	@Override
	public String toString() {
		return "BitfinexOrderBookResponse [bids=" + bids + ", asks=" + asks + "]";
	}

	public static class BitfinexOrderBookEntryResponse extends OrderBookResponseEntry {
		
		private BigDecimal price;
		private BigDecimal amount;
		private String timestamp;
		
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

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

		@Override
		public String toString() {
			return "BitfinexOrderBookEntryResponse [price=" + price + ", amount=" + amount + ", timestamp=" + timestamp + "]";
		}
		
	}
	
}
