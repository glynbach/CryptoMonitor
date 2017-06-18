package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.util.Collections;
import java.util.List;

import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;

public class BittrexAccountsResponseContainer extends BittrexResponseContainer<List<BittrexAccountResponse>> implements AccountsResponse {

	@Override
	public List<? extends AccountResponse> getAccountResponses() {
		return this.getResult() == null ? Collections.emptyList() : this.getResult().subList(0, this.getResult().size());
	}
	

}
