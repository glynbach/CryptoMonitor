package com.kieral.cryptomon.service.exchange.poloniex;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexAccountResponse {

	private BigDecimal available;
	private BigDecimal onOrders;
	
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
	
}
