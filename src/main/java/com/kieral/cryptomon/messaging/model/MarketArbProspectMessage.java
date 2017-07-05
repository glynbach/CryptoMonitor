package com.kieral.cryptomon.messaging.model;

import java.util.List;

public class MarketArbProspectMessage {

	private String market;
	private List<MarketCurrencyArbProspectMessage> currencyProspects;

	public MarketArbProspectMessage() {
	}
	
	public MarketArbProspectMessage(String market, List<MarketCurrencyArbProspectMessage> currencyProspects) {
		this.market = market;
		this.currencyProspects = currencyProspects;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public List<MarketCurrencyArbProspectMessage> getCurrencyProspects() {
		return currencyProspects;
	}

	public void setCurrencyProspects(List<MarketCurrencyArbProspectMessage> currencyProspects) {
		this.currencyProspects = currencyProspects;
	}

	@Override
	public String toString() {
		return "MarketArbProspectMessage [market=" + market + ", currencyProspects=" + currencyProspects + "]";
	}
	
}
