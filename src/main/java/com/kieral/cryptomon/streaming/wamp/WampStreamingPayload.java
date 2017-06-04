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
	private final String topic;
	private final String currencyPair;
	
	protected WampStreamingPayload(PubSubData payload, String topic, String currencyPair) {
		if (payload == null)
			throw new IllegalArgumentException("payLoad can not be null");
		this.payload = payload;
		this.topic = topic;
		this.currencyPair = currencyPair;
	}

	@Override
	public String getTopic() {
		return topic;
	}

	@Override
	public long getSequenceNumber() {
		final AtomicLong seqNum = new AtomicLong(-1);
		try {
			payload.keywordArguments().findValuesAsText("seq").forEach(seq -> {seqNum.set(Long.parseLong(seq));});
		} catch (Exception e) {
			logger.warn("Could not get sequence number from {} for {}", payload.keywordArguments(), payload.arguments(), e);
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currencyPair == null) ? 0 : currencyPair.hashCode());
		result = prime * result + ((payload == null) ? 0 : payload.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WampStreamingPayload other = (WampStreamingPayload) obj;
		if (currencyPair == null) {
			if (other.currencyPair != null)
				return false;
		} else if (!currencyPair.equals(other.currencyPair))
			return false;
		if (payload == null) {
			if (other.payload != null)
				return false;
		} else if (!payload.equals(other.payload))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "WampStreamingPayload [getTopic()=" + getTopic() + ", getSequenceNumber()=" + getSequenceNumber()
				+ ", getCurrencyPair()=" + getCurrencyPair() + ", getJson()=" + getJson() + "]";
	}

	@Override
	public String getRaw() {
		return payload == null ? null : payload.keywordArguments() + " " + payload.details() + " " + payload.arguments();
	}

}
