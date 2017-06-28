package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.trading.AmountHolder;

public interface TradeResponse extends AmountHolder {

	String getTradeId();
	BigDecimal getRate();
	BigDecimal getFee();
	long getTradeTime();
	boolean isFeeInQuotedCurrency();
	
}
