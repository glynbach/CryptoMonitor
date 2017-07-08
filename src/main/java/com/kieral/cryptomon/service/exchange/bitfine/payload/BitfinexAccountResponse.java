package com.kieral.cryptomon.service.exchange.bitfine.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.rest.AccountResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitfinexAccountResponse implements AccountResponse {

	private String type;
	private String currency;
	private BigDecimal amount;
	private BigDecimal available;
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	public BigDecimal getAvailable() {
		return available;
	}
	
	public void setAvailable(BigDecimal available) {
		this.available = available;
	}

	@Override
	public String toString() {
		return "BitfinexAccountResponse [type=" + type + ", currency=" + currency + ", amount=" + amount
				+ ", available=" + available + "]";
	}

	@Override
	public String getAccountId() {
		return currency + "-account";
	}

	@Override
	public String getAccountAddress() {
		return null;
	}

	@Override
	public Currency getAccountCurrency() {
		return Currency.valueOf(currency);
	}

	@Override
	public BigDecimal getAvailableBalance() {
		return available;
	}

	@Override
	public BigDecimal getPendingBalance() {
		return amount.subtract(available);
	}
	
}
