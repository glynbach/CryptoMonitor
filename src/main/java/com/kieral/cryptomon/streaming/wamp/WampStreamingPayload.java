package com.kieral.cryptomon.streaming.wamp;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.streaming.StreamingPayload;

import ws.wamp.jawampa.PubSubData;

public class WampStreamingPayload implements StreamingPayload {

	private final static Logger logger = LoggerFactory.getLogger(WampStreamingPayload.class);
	
	private final PubSubData payload;
	private final String currencyPair;
	
	protected WampStreamingPayload(PubSubData payload, String currencyPair) {
		if (payload == null)
			throw new IllegalArgumentException("payLoad can not be null");
		this.payload = payload;
		this.currencyPair = currencyPair;
	}
	
	@Override
	public long getSequenceNumber() {
		final AtomicLong seqNum = new AtomicLong(-1);
		try {
			payload.keywordArguments().findValuesAsText("seq").forEach(seq -> {seqNum.set(Long.parseLong(seq));});
		} catch (Exception e) {
			logger.warn("Could not get sequence number from " + payload.keywordArguments() + " for " + payload.arguments(), e);
		}
		return seqNum.get();
	}

	@Override
	public String getCurrencyPair() {
		return currencyPair;
	}

	@Override
	public JsonNode getJson() {
		return payload.arguments();
	}

	@Override
	public String toString() {
		return "WampStreamingPayload [getSequenceNumber()=" + getSequenceNumber() + ", getCurrencyPair()="
				+ getCurrencyPair() + ", getJson()=" + getJson() + "]";
	}

}
