package com.kieral.cryptomon.service.exchange.gdax;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxAccountsResponse extends AccountsResponse {

	private List<GdaxAccountResponse> accountResponses;

	public List<GdaxAccountResponse> getAccountResponses() {
		return accountResponses;
	}

	public void setAccountResponses(List<GdaxAccountResponse> accountResponses) {
		this.accountResponses = accountResponses;
	}

	@Override
	public String toString() {
		return "GdaxAccountsResponse [accountResponses=" + accountResponses + "]";
	}
	
}
