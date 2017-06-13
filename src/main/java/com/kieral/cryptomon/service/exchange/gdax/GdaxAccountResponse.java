package com.kieral.cryptomon.service.exchange.gdax;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxAccountResponse {

	private String id;
	private String currency;
	private BigDecimal balance;
	private BigDecimal available;
	private BigDecimal hold;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getCurrency() {
		return currency;
	}
	
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	public BigDecimal getBalance() {
		return balance;
	}
	
	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
	
	public BigDecimal getAvailable() {
		return available;
	}
	
	public void setAvailable(BigDecimal available) {
		this.available = available;
	}
	
	public BigDecimal getHold() {
		return hold;
	}
	
	public void setHold(BigDecimal hold) {
		this.hold = hold;
	}

	@Override
	public String toString() {
		return "GdaxAccountResponse [id=" + id + ", currency=" + currency + ", balance=" + balance + ", available="
				+ available + ", hold=" + hold + "]";
	}
	
}
