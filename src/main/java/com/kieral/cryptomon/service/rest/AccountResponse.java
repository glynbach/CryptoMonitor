package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Currency;

public interface AccountResponse {

	String getAccountId();
	String getAccountAddress();
	Currency getAccountCurrency();
	BigDecimal getAvailableBalance();
	BigDecimal getPendingBalance();
	
}
