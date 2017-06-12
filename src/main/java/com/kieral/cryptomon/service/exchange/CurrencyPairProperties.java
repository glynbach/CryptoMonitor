package com.kieral.cryptomon.service.exchange;

public class CurrencyPairProperties {

	private String pair;
	private int priceScale;
	private String tradingFee;
	
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

	@Override
	public String toString() {
		return "CurrencyPair [pair=" + pair + ", priceScale=" + priceScale + ", tradingFee=" + tradingFee + "]";
	}
	
}
