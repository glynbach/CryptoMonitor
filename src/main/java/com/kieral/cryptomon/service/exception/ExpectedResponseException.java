package com.kieral.cryptomon.service.exception;

import org.springframework.http.HttpStatus;

import com.kieral.cryptomon.service.exchange.ExchangeApiRequest.ResponseErrorAction;

public class ExpectedResponseException extends Exception {

	private static final long serialVersionUID = -276593584177638388L;
	
	private final ResponseErrorAction action;
	private final HttpStatus status; 
	private final String responseBody;

	public ExpectedResponseException(Exception e, ResponseErrorAction action, HttpStatus status, String responseBody) {
		super(e);
		this.action = action;
		this.status = status;
		this.responseBody = responseBody;
	}

	public ExpectedResponseException(ResponseErrorAction action, HttpStatus status, String responseBody) {
		super();
		this.action = action;
		this.status = status;
		this.responseBody = responseBody;
	}

	public ResponseErrorAction getAction() {
		return action;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getResponseBody() {
		return responseBody;
	}

}
