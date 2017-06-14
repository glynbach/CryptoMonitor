package com.kieral.cryptomon.service.exchange.gdax;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxAccountsResponse extends ArrayList<GdaxAccountResponse> implements AccountsResponse {

	private static final long serialVersionUID = -4951530146668293126L;

	public List<GdaxAccountResponse> getAccountResponses() {
		return new ArrayList<GdaxAccountResponse>(this.subList(0, this.size()));
	}

	@Override
	public String toString() {
		return "GdaxAccountsResponse [accountResponses=" + getAccountResponses() + "]";
	}
	
}
