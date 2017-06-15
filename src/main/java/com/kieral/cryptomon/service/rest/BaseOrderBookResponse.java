package com.kieral.cryptomon.service.rest;

import com.kieral.cryptomon.model.general.CurrencyPair;

public abstract class BaseOrderBookResponse implements OrderBookResponse {

	protected CurrencyPair currencyPair;
	
	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}
	
	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

}
