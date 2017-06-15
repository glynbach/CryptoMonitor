package com.kieral.cryptomon.service.rest;

import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;

public interface OrderBookResponse {

	CurrencyPair getCurrencyPair();
	void setCurrencyPair(CurrencyPair currencyPair);
	long getSequence();
	long getCreatedTime();
	boolean isValid();
	List<OrderBookResponseEntry> getBidEntries(); 
	List<OrderBookResponseEntry> getAskEntries();
	
}
