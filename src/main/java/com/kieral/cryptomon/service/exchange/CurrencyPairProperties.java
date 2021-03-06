package com.kieral.cryptomon.service.exchange;

import java.math.BigDecimal;

public class CurrencyPairProperties {

	private String pair;
	private int priceScale;
	private String tradingFee;
	private BigDecimal minDesiredBaseBalance;
	private BigDecimal minDesiredQuotedBalance;
	
	public CurrencyPairProperties() {
	}
	
	public CurrencyPairProperties(String pair, int priceScale, String tradingFee) {
		this.pair = pair;
		this.priceScale = priceScale;
		this.tradingFee = tradingFee;
	}

	public String getPair() {
		return pair;
	}

	public void setPair(String pair) {
		this.pair = pair;
	}

	public int getPriceScale() {
		return priceScale;
	}

	public void setPriceScale(int priceScale) {
		this.priceScale = priceScale;
	}

	public String getTradingFee() {
		return tradingFee;
	}

	public void setTradingFee(String tradingFee) {
		this.tradingFee = tradingFee;
	}

	public BigDecimal getMinDesiredBaseBalance() {
		return minDesiredBaseBalance;
	}

	public void setMinDesiredBaseBalance(BigDecimal minDesiredBaseBalance) {
		this.minDesiredBaseBalance = minDesiredBaseBalance;
	}

	public BigDecimal getMinDesiredQuotedBalance() {
		return minDesiredQuotedBalance;
	}

	public void setMinDesiredQuotedBalance(BigDecimal minDesiredQuotedBalance) {
		this.minDesiredQuotedBalance = minDesiredQuotedBalance;
	}

	@Override
	public String toString() {
		return "CurrencyPairProperties [pair=" + pair + ", priceScale=" + priceScale + ", tradingFee=" + tradingFee
				+ ", minDesiredBaseBalance=" + minDesiredBaseBalance + ", minDesiredQuotedBalance="
				+ minDesiredQuotedBalance + "]";
	}
	
}
