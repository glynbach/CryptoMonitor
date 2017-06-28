package com.kieral.cryptomon.model.trading;

import java.math.BigDecimal;

public class Trade implements AmountHolder {

	private final String tradeId;
	private final BigDecimal rate;
	private final BigDecimal amount;
	private final BigDecimal fee;
	private final boolean feeOnQuotedCurrency;
	private final long tradeTime;
	
	public Trade(String tradeId, BigDecimal rate, BigDecimal amount, BigDecimal fee, boolean feeOnQuotedCurrency,
			long tradeTime) {
		this.tradeId = tradeId;
		this.rate = rate;
		this.amount = amount;
		this.fee = fee;
		this.feeOnQuotedCurrency = feeOnQuotedCurrency;
		this.tradeTime = tradeTime;
	}

	public String getTradeId() {
		return tradeId;
	}

	public BigDecimal getRate() {
		return rate;
	}

	@Override
	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public boolean isFeeOnQuotedCurrency() {
		return feeOnQuotedCurrency;
	}

	public long getTradeTime() {
		return tradeTime;
	}

	@Override
	public String toString() {
		return "Trade [tradeId=" + tradeId + ", rate=" + rate + ", amount=" + amount + ", fee=" + fee
				+ ", feeOnQuotedCurrency=" + feeOnQuotedCurrency + ", tradeTime=" + tradeTime + "]";
	}
	
}
