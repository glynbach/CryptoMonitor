package com.kieral.cryptomon.messaging;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
//		String jsonInString = "{\"market\":\"Poloniex\"}";
		String jsonInString = "{\"market\":\"Poloniex\"}";

		//JSON from String to Object
		SubscriptionMessage subscriptionMessage = mapper.readValue(jsonInString, SubscriptionMessage.class);
		System.out.println(subscriptionMessage);
	}
}
