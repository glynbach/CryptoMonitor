package com.kieral.cryptomon.service.exception;

import com.kieral.cryptomon.service.exchange.ExchangeApiRequest;

public class ResponseTimedOutException extends Exception {

	private static final long serialVersionUID = -5681768073318492885L;
	
	private final ExchangeApiRequest apiRequest;
	private final String descr;
	
	public ResponseTimedOutException(ExchangeApiRequest apiRequest, String descr) {
		super();
		this.apiRequest = apiRequest;
		this.descr = descr;
	}

	public ExchangeApiRequest getApiRequest() {
		return apiRequest;
	}

	public String getDescr() {
		return descr;
	}
	
}
