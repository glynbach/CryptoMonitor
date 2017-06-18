package com.kieral.cryptomon.messaging.model;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.service.util.CommonUtils;

public class OrderBookMessage {

	private final static int MAX_DEPTH = 5;
	
	private String market;
	private String currencyPair;
	private List<OrderBookRow> obEntries = new ArrayList<OrderBookRow>();
	private String lastUpdated;
	private boolean valid;
	
	public OrderBookMessage() {
	}

	public OrderBookMessage(String market, String currencyPair, List<OrderBookRow> obEntries, long lastUpdated,
			boolean valid) {
		super();
		this.market = market;
		this.currencyPair = currencyPair;
		this.obEntries = obEntries;
		this.lastUpdated = CommonUtils.SECONDS_FORMATTER.format(Instant.ofEpochMilli(lastUpdated)
				.atZone(ZoneId.systemDefault()).toLocalDateTime());
		this.valid = valid;
	}

	public OrderBookMessage(OrderBook orderBook) {
		if (orderBook != null) {
			this.market = orderBook.getMarket();
			this.currencyPair = orderBook.getCurrencyPair().getName();
			obEntries = new ArrayList<OrderBookRow>();
			for (int i=0; i<orderBook.getBids().size() && i<MAX_DEPTH; i++) {
				if (obEntries.size() <= i)
					obEntries.add(new OrderBookRow());
				obEntries.get(i).setBidPrice(orderBook.getBids().get(i).getPrice().toPlainString());
				obEntries.get(i).setBidAmount(orderBook.getBids().get(i).getAmount().toPlainString());
			}
			for (int i=0; i<orderBook.getAsks().size() && i<MAX_DEPTH; i++) {
				if (obEntries.size() <= i)
					obEntries.add(new OrderBookRow());
				obEntries.get(i).setAskPrice(orderBook.getAsks().get(i).getPrice().toPlainString());
				obEntries.get(i).setAskAmount(orderBook.getAsks().get(i).getAmount().toPlainString());
			}
			obEntries.forEach(row -> {
				if (row.getBidPrice() == null) row.setBidPrice("");
				if (row.getBidAmount() == null) row.setBidAmount("");
				if (row.getAskPrice() == null) row.setAskPrice("");
				if (row.getAskAmount() == null) row.setAskAmount("");
			});
		}
		this.lastUpdated = CommonUtils.SECONDS_FORMATTER.format(Instant.ofEpochMilli(orderBook.getSnapshotReceived())
				.atZone(ZoneId.systemDefault()).toLocalDateTime());
		// TOOO: implement OB frozen in the model 
		this.valid = true;
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

	public List<OrderBookRow> getObEntries() {
		return obEntries;
	}

	public void setObEntries(List<OrderBookRow> obEntries) {
		this.obEntries = obEntries;
	}

	public String getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public String toString() {
		return "OrderBookMessage [market=" + market + ", currencyPair=" + currencyPair + ", obEntries=" + obEntries
				+ ", lastUpdated=" + lastUpdated + ", valid=" + valid + "]";
	}
	
}
