package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.rest.AccountResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexAccountResponse implements AccountResponse {

	private BigDecimal available;
	private BigDecimal onOrders;
	
	protected String id;
	protected Currency currency;
	protected String address;
	
	public BigDecimal getAvailable() {
		return available;
	}
	
	public void setAvailable(BigDecimal available) {
		this.available = available;
	}
	
	public BigDecimal getOnOrders() {
		return onOrders;
	}
	
	public void setOnOrders(BigDecimal onOrders) {
		this.onOrders = onOrders;
	}

	@Override
	public String toString() {
		return "PoloniexAccountResponse [available=" + available + ", onOrders=" + onOrders + "]";
	}

	@Override
	public BigDecimal getAvailableBalance() {
		return available;
	}

	@Override
	public BigDecimal getPendingBalance() {
		return onOrders;
	}

	protected void setId(String id) {
		this.id = id;
	}

	protected void setCurrency(Currency currency) {
		this.currency = currency;
	}

	protected void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String getAccountId() {
		return id;
	}

	@Override
	public String getAccountAddress() {
		return address;
	}

	@Override
	public Currency getAccountCurrency() {
		return currency;
	}


}
