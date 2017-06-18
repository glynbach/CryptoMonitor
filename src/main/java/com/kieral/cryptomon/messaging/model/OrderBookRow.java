package com.kieral.cryptomon.messaging.model;

public class OrderBookRow {

	private String bidPrice;
	private String bidAmount;
	private String askPrice;
	private String askAmount;

	public OrderBookRow() {
	}

	public OrderBookRow(String bidPrice, String bidAmount, String askPrice, String askAmount) {
		this.bidPrice = bidPrice;
		this.bidAmount = bidAmount;
		this.askPrice = askPrice;
		this.askAmount = askAmount;
	}

	public String getBidPrice() {
		return bidPrice;
	}
	
	public void setBidPrice(String bidPrice) {
		this.bidPrice = bidPrice;
	}
	
	public String getBidAmount() {
		return bidAmount;
	}
	
	public void setBidAmount(String bidAmount) {
		this.bidAmount = bidAmount;
	}
	
	public String getAskPrice() {
		return askPrice;
	}
	
	public void setAskPrice(String askPrice) {
		this.askPrice = askPrice;
	}
	
	public String getAskAmount() {
		return askAmount;
	}
	
	public void setAskAmount(String askAmount) {
		this.askAmount = askAmount;
	}

	@Override
	public String toString() {
		return "OrderBookRow [bidPrice=" + bidPrice + ", bidAmount=" + bidAmount + ", askPrice=" + askPrice
				+ ", askAmount=" + askAmount + "]";
	}
	
}
