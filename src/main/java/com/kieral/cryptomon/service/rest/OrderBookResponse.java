package com.kieral.cryptomon.service.rest;

import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;

public abstract class OrderBookResponse {

	protected CurrencyPair currencyPair;
	
	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}
	
	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}
	
	public abstract long getSequence();
	public abstract long getCreatedTime();
	public abstract boolean isValid();
	public abstract List<OrderBookResponseEntry> getBidEntries(); 
	public abstract List<OrderBookResponseEntry> getAskEntries();
	
}
