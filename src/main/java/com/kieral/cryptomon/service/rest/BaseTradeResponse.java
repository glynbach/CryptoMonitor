package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;

public class BaseTradeResponse implements TradeResponse {

	private final String tradeId;
	private final BigDecimal rate, amount, fee;
	private final long tradeTime;
	private final boolean feeInQuotedCurrency;

	public BaseTradeResponse(String tradeId, BigDecimal rate, BigDecimal amount, BigDecimal fee, 
			long tradeTime, boolean feeInQuotedCurrency) {
		this.tradeId = tradeId;
		this.rate = rate;
		this.amount = amount == null ? BigDecimal.ZERO : amount;
		this.fee = fee == null ? BigDecimal.ZERO : fee;
		this.tradeTime = tradeTime;
		this.feeInQuotedCurrency = feeInQuotedCurrency;
	}

	@Override
	public String getTradeId() {
		return tradeId;
	}

	@Override
	public BigDecimal getRate() {
		return rate;
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	@Override
	public BigDecimal getFee() {
		return fee;
	}

	public long getTradeTime() {
		return tradeTime;
	}

	@Override
	public boolean isFeeInQuotedCurrency() {
		return feeInQuotedCurrency;
	}

	@Override
	public String toString() {
		return "BaseTradeResponse [tradeId=" + tradeId + ", rate=" + rate + ", amount=" + amount + ", fee=" + fee
				+ ", tradeTime=" + tradeTime + ", feeInQuotedCurrency=" + feeInQuotedCurrency + "]";
	}
	
}
