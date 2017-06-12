package com.kieral.cryptomon.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.general.CurrencyPair;

public interface StreamingPayload {

	long getSequenceNumber();
	long getTimeReceived();
	CurrencyPair getCurrencyPair();
	JsonNode getJson();
	String getRaw();
	boolean isHeartbeat();
	
}
