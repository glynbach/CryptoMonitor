package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.trading.AmountValue;

public interface TradeResponse extends AmountValue {

	String getTradeId();
	BigDecimal getRate();
	BigDecimal getFee();
	long getTradeTime();
	boolean isFeeInQuotedCurrency();
	
}
