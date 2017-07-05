package com.kieral.cryptomon.messaging.model;

import java.util.List;

public class MarketCurrencyArbProspectMessage {
	
	private String market;
	private String currencyPair;
	private String bestAskPrice;
	private List<MarketBidPriceMessage> marketBids;
	
	public MarketCurrencyArbProspectMessage() {
	}
	
	public MarketCurrencyArbProspectMessage(String market, String currencyPair, String bestAskPrice, List<MarketBidPriceMessage> marketBids) {
		this.market = market;
		this.currencyPair = currencyPair;
		this.bestAskPrice = bestAskPrice;
		this.marketBids = marketBids;
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

	public String getBestAskPrice() {
		return bestAskPrice;
	}

	public void setBestAskPrice(String bestAskPrice) {
		this.bestAskPrice = bestAskPrice;
	}

	public List<MarketBidPriceMessage> getMarketBids() {
		return marketBids;
	}

	public void setMarketBids(List<MarketBidPriceMessage> marketBids) {
		this.marketBids = marketBids;
	}

	@Override
	public String toString() {
		return "MarketArbProspectMessage [market=" + market + ", currencyPair=" + currencyPair + ", bestAskPrice="
				+ bestAskPrice + ", marketBids=" + marketBids + "]";
	}
	
}
