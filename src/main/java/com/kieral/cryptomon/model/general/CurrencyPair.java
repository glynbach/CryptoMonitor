package com.kieral.cryptomon.model.general;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.accounting.TradingFeeType;

public class CurrencyPair {

	private final String name;
	private final Currency baseCurrency;
	private final Currency quotedCurrency;
	private final String topic;
	private final int priceScale;
	private final BigDecimal tradingFee;
	private final TradingFeeType tradingFeeType;

	/**
	 * E.g. LTC/BTC 
	 * LTC is base currency 
	 * BTC is quoted currency
	 * price of 0.091 means 1 LTC is exchanged for 0.091 BTC 
	 */
	public CurrencyPair(String name, Currency baseCurrency, Currency quotedCurrency, String topic,
			int priceScale, BigDecimal tradingFee, TradingFeeType tradingFeeType) {
		this.name = name;
		this.baseCurrency = baseCurrency;
		this.quotedCurrency = quotedCurrency;
		this.topic = topic;
		this.priceScale = priceScale;
		this.tradingFee = tradingFee;
		this.tradingFeeType = tradingFeeType;
	}

	public CurrencyPair(String name, String topic) {
		this.name = name;
		this.topic = topic;
		this.baseCurrency = null;
		this.quotedCurrency = null;
		this.priceScale = 0;
		this.tradingFee = null;
		this.tradingFeeType = null;
	}

	public String getName() {
		return name;
	}

	public Currency getBaseCurrency() {
		return baseCurrency;
	}

	public Currency getQuotedCurrency() {
		return quotedCurrency;
	}

	public String getTopic() {
		return topic;
	}

	public int getPriceScale() {
		return priceScale;
	}

	public BigDecimal getTradingFee() {
		return tradingFee;
	}

	public TradingFeeType getTradingFeeType() {
		return tradingFeeType;
	}

	@Override
	public String toString() {
		return "CurrencyPair [name=" + name + ", baseCurrency=" + baseCurrency + ", quotedCurrency="
				+ quotedCurrency + ", topic=" + topic + ", priceScale=" + priceScale + ", tradingFee=" + tradingFee
				+ ", tradingFeeType=" + tradingFeeType + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseCurrency == null) ? 0 : baseCurrency.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((quotedCurrency == null) ? 0 : quotedCurrency.hashCode());
		result = prime * result + priceScale;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		result = prime * result + ((tradingFee == null) ? 0 : tradingFee.hashCode());
		result = prime * result + ((tradingFeeType == null) ? 0 : tradingFeeType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CurrencyPair other = (CurrencyPair) obj;
		if (baseCurrency != other.baseCurrency)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (quotedCurrency != other.quotedCurrency)
			return false;
		if (priceScale != other.priceScale)
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		if (tradingFee == null) {
			if (other.tradingFee != null)
				return false;
		} else if (!tradingFee.equals(other.tradingFee))
			return false;
		if (tradingFeeType != other.tradingFeeType)
			return false;
		return true;
	}
	
}
