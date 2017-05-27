package com.kieral.cryptomon.streaming;

import com.fasterxml.jackson.databind.JsonNode;

public interface StreamingPayload {

	String getTopic();
	long getSequenceNumber();
	String getCurrencyPair();
	JsonNode getJson();
	
}
