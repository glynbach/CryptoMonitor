package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

public interface TradeResponse {

	String getTradeId();
	BigDecimal getRate();
	BigDecimal getAmount();
	BigDecimal getFee();
	long getTradeTime();
	boolean isFeeInQuotedCurrency();
	
}
