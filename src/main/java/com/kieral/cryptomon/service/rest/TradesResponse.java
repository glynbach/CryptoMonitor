package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.trading.Trade;

public interface TradesResponse {

	List<? extends TradeResponse> getTradeResponses();
	List<Trade> getTrades();
	BigDecimal getFilledAmount();
	int getNumTrades();
	boolean isPlaceholder();
	
}
