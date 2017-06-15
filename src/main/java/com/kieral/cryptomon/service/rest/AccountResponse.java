package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Currency;

public interface AccountResponse {

	String getAccountId();
	String getAccountAddress();
	Currency getAccountCuurency();
	BigDecimal getAvailableBalance();
	BigDecimal getPendingBalance();
	
}
