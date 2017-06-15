package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrderResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderResponse implements OrderResponse {

	private String orderNumber;
	private String type;
	private BigDecimal rate;
	private BigDecimal amount;
	private BigDecimal total;
	
	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	@Override
	public String toString() {
		return "PoloniexOrderResponse [orderNumber=" + orderNumber + ", type=" + type + ", rate=" + rate + ", amount="
				+ amount + ", total=" + total + "]";
	}
	
}
