package com.kieral.cryptomon.service.exchange.bittrex;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BittrexOrderBookResponse extends OrderBookResponse {

	private final Long createdTime = System.currentTimeMillis();
	private boolean success;
	private String message;
	private Result result;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}

	@Override
	public long getCreatedTime() {
		return createdTime;
	}

	@Override
	public long getSequence() {
		return OrderBookManager.AUTO_INCREMENT_SEQUENCE;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public List<OrderBookResponseEntry> getBidEntries() {
		return getEntries(Side.BID);
	}

	@Override
	public List<OrderBookResponseEntry> getAskEntries() {
		return getEntries(Side.ASK);
	}

	private List<OrderBookResponseEntry> getEntries(Side side) {
		List<OrderBookResponseEntry> rtn = new ArrayList<OrderBookResponseEntry>();
		if (result != null) {
			if (side == Side.BID) {
				if (result.getBuy() != null) {
					result.getBuy().forEach(entry -> {
						rtn.add(new OrderBookResponseEntry(entry.getRate(), 
								entry.getQuantity(), 0));
					});
				}
			} else { 
				if (result.getSell() != null) {
					result.getSell().forEach(entry -> {
						rtn.add(new OrderBookResponseEntry(entry.getRate(), 
								entry.getQuantity(), 0));
					});
				}
			}
		}
		return rtn;
	}

	public static final class Result {
		
		private List<Entry> buy;
		private List<Entry> sell;
		
		public List<Entry> getBuy() {
			return buy;
		}

		public void setBuy(List<Entry> buy) {
			this.buy = buy;
		}

		public List<Entry> getSell() {
			return sell;
		}

		public void setSell(List<Entry> sell) {
			this.sell = sell;
		}

		@Override
		public String toString() {
			return "Result [buy=" + buy + ", sell=" + sell + "]";
		}
		
	}
	
	public static final class Entry {
		
		private BigDecimal quantity;
		private BigDecimal rate;
		
		public BigDecimal getQuantity() {
			return quantity;
		}

		@JsonProperty("Quantity")
		public void setQuantity(BigDecimal quantity) {
			this.quantity = quantity;
		}

		public BigDecimal getRate() {
			return rate;
		}

		@JsonProperty("Rate")
		public void setRate(BigDecimal rate) {
			this.rate = rate;
		}

		@Override
		public String toString() {
			return "Entry [quantity=" + quantity + ", rate=" + rate + "]";
		}
		
	}

	@Override
	public String toString() {
		return "BittrexOrderBookResponse [createdTime=" + createdTime + ", success=" + success + ", message=" + message
				+ ", result=" + result + "]";
	}
	
}
