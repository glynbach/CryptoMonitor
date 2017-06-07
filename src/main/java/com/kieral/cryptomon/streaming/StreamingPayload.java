package com.kieral.cryptomon.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.CurrencyPair;

public interface StreamingPayload {

	long getSequenceNumber();
	CurrencyPair getCurrencyPair();
	JsonNode getJson();
	String getRaw();
	boolean isHeartbeat();
	
}
