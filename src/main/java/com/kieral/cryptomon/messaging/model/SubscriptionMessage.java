package com.kieral.cryptomon.messaging.model;

public class SubscriptionMessage {

	private String market;
	
	public SubscriptionMessage() {
	}
	
	public SubscriptionMessage(String market) {
		this.market = market;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	@Override
	public String toString() {
		return "SubscriptionMessage [market=" + market + "]";
	}

}
