package com.kieral.cryptomon.service.exchange.yobit.payload;

import java.util.List;

import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;

public class YobitAccountsResponseContainer extends YobitResponseContainer<YobitAccountsResponse> implements AccountsResponse {

	@Override
	public List<? extends AccountResponse> getAccountResponses() {
		return this.getReturnValue().getAccountResponses();
	}

	@Override
	public String toString() {
		return "YobitAccountsResponseContainer [success=" + success + ", returnValue=" + returnValue + "]";
	}

}
