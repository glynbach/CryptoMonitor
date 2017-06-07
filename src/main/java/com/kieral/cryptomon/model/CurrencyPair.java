package com.kieral.cryptomon.model;

import java.math.BigDecimal;

import com.google.common.annotations.VisibleForTesting;

public class CurrencyPair {

	private final String name;
	private final Currency majorCurrency;
	private final Currency otherCurrency;
	private final String topic;
	private final int priceScale;
	private final BigDecimal tradingFee;
	private final String tradingFeeCurrency;
	
	public CurrencyPair(String name, Currency majorCurrency, Currency otherCurrency, String topic,
			int priceScale, BigDecimal tradingFee, String tradingFeeCurrency) {
		this.name = name;
		this.majorCurrency = majorCurrency;
		this.otherCurrency = otherCurrency;
		this.topic = topic;
		this.priceScale = priceScale;
		this.tradingFee = tradingFee;
		this.tradingFeeCurrency = tradingFeeCurrency;
	}

	@VisibleForTesting
	public CurrencyPair(String name, String topic) {
		this.name = name;
		this.topic = topic;
		this.majorCurrency = null;
		this.otherCurrency = null;
		this.priceScale = 0;
		this.tradingFee = null;
		this.tradingFeeCurrency = null;
	}

	public String getName() {
		return name;
	}

	public Currency getMajorCurrency() {
		return majorCurrency;
	}

	public Currency getOtherCurrency() {
		return otherCurrency;
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

	public String getTradingFeeCurrency() {
		return tradingFeeCurrency;
	}

	@Override
	public String toString() {
		return "CurrencyPair [name=" + name + ", majorCurrency=" + majorCurrency + ", otherCurrency="
				+ otherCurrency + ", topic=" + topic + ", priceScale=" + priceScale + ", tradingFee=" + tradingFee
				+ ", tradingFeeCurrency=" + tradingFeeCurrency + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((majorCurrency == null) ? 0 : majorCurrency.hashCode());
		result = prime * result + ((otherCurrency == null) ? 0 : otherCurrency.hashCode());
		result = prime * result + priceScale;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		result = prime * result + ((tradingFee == null) ? 0 : tradingFee.hashCode());
		result = prime * result + ((tradingFeeCurrency == null) ? 0 : tradingFeeCurrency.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (majorCurrency != other.majorCurrency)
			return false;
		if (otherCurrency != other.otherCurrency)
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
		if (tradingFeeCurrency == null) {
			if (other.tradingFeeCurrency != null)
				return false;
		} else if (!tradingFeeCurrency.equals(other.tradingFeeCurrency))
			return false;
		return true;
	}
	
}
