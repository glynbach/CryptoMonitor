package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.rest.AccountResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BittrexAccountResponse implements AccountResponse {

	private String cryptoAddress;
	private String currency;
	private BigDecimal balance;
	private BigDecimal available;
	private BigDecimal pending;
	
	public String getCryptoAddress() {
		return cryptoAddress;
	}
	
	@JsonProperty("CryptoAddress")
	public void setCryptoAddress(String cryptoAddress) {
		this.cryptoAddress = cryptoAddress;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	@JsonProperty("Currency")
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public BigDecimal getBalance() {
		return balance;
	}
	
	@JsonProperty("Balance")
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
	
	public BigDecimal getAvailable() {
		return available;
	}
	
	@JsonProperty("Available")
	public void setAvailable(BigDecimal available) {
		this.available = available;
	}
	
	public BigDecimal getPending() {
		return pending;
	}
	
	@JsonProperty("Pending")
	public void setPending(BigDecimal pending) {
		this.pending = pending;
	}

	@Override
	public String toString() {
		return "BittrexAccountResponse [cryptoAddress=" + cryptoAddress + ", currency=" + currency + ", balance="
				+ balance + ", available=" + available + ", pending=" + pending + "]";
	}

	@Override
	public String getAccountId() {
		return cryptoAddress;
	}

	@Override
	public String getAccountAddress() {
		return cryptoAddress;
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
		return pending;
	}
	
}
