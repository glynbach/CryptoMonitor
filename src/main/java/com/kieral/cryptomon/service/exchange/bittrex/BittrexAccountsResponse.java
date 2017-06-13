package com.kieral.cryptomon.service.exchange.bittrex;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BittrexAccountsResponse extends AccountsResponse {

	private boolean success;
	private String message;
	private List<BittrexAccountResponse> accountResponses;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<BittrexAccountResponse> getAccountResponses() {
		return accountResponses;
	}

	public void setAccountResponses(List<BittrexAccountResponse> accountResponses) {
		this.accountResponses = accountResponses;
	}

	@Override
	public String toString() {
		return "BittrexAccountsResponse [success=" + success + ", message=" + message + ", accountResponses="
				+ accountResponses + "]";
	}
	
}
