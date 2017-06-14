package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Currency;

public interface AccountResponse {

	public abstract String getAccountId();
	public abstract String getAccountAddress();
	public abstract Currency getAccountCuurency();
	public abstract BigDecimal getAvailableBalance();
	public abstract BigDecimal getPendingBalance();
	
}
