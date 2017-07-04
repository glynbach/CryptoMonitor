package com.kieral.cryptomon.messaging.model;

public class MarketBidPriceMessage {

	private String market;
	private String bestBidPrice;
	private String amountAvailable;
	private String quotedProfit;
	private String quotedProfitWithFees;
	private boolean opportunity;
	
	public MarketBidPriceMessage() {
	}

	public MarketBidPriceMessage(String market, String bestBidPrice, String amountAvailable, String quotedProfit,
			String quotedProfitWithFees, boolean opportunity) {
		this.market = market;
		this.bestBidPrice = bestBidPrice;
		this.amountAvailable = amountAvailable;
		this.quotedProfit = quotedProfit;
		this.quotedProfitWithFees = quotedProfitWithFees;
		this.opportunity = opportunity;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getBestBidPrice() {
		return bestBidPrice;
	}

	public void setBestBidPrice(String bestBidPrice) {
		this.bestBidPrice = bestBidPrice;
	}

	public String getAmountAvailable() {
		return amountAvailable;
	}

	public void setAmountAvailable(String amountAvailable) {
		this.amountAvailable = amountAvailable;
	}

	public String getQuotedProfit() {
		return quotedProfit;
	}

	public void setQuotedProfit(String quotedProfit) {
		this.quotedProfit = quotedProfit;
	}

	public String getQuotedProfitWithFees() {
		return quotedProfitWithFees;
	}

	public void setQuotedProfitWithFees(String quotedProfitWithFees) {
		this.quotedProfitWithFees = quotedProfitWithFees;
	}

	public boolean isOpportunity() {
		return opportunity;
	}

	public void setOpportunity(boolean opportunity) {
		this.opportunity = opportunity;
	}

	@Override
	public String toString() {
		return "MarketAskPriceMessage [market=" + market + ", bestBidPrice=" + bestBidPrice + ", amountAvailable="
				+ amountAvailable + ", quotedProfit=" + quotedProfit + ", quotedProfitWithFees="
				+ quotedProfitWithFees + ", opportunity=" + opportunity + "]";
	}
	
}
