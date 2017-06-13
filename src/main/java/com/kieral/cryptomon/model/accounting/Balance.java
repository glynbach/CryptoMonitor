package com.kieral.cryptomon.model.accounting;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Currency;

public class Balance {

	private final String market;
	private final Currency currency;
	private BigDecimal confirmedAmount;
	private BigDecimal workingAmount;
	
	public Balance(String market, Currency currency, BigDecimal confirmedAmount, BigDecimal workingAmount) {
		this.market = market;
		this.currency = currency;
		this.confirmedAmount = confirmedAmount;
		this.workingAmount = workingAmount;
	}

	public BigDecimal getConfirmedAmount() {
		return confirmedAmount;
	}

	public void setConfirmedAmount(BigDecimal confirmedAmount) {
		this.confirmedAmount = confirmedAmount;
	}

	public BigDecimal getWorkingAmount() {
		return workingAmount;
	}

	public void setWorkingAmount(BigDecimal workingAmount) {
		this.workingAmount = workingAmount;
	}

	public String getMarket() {
		return market;
	}

	public Currency getCurrency() {
		return currency;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currency == null) ? 0 : currency.hashCode());
		result = prime * result + ((market == null) ? 0 : market.hashCode());
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
		Balance other = (Balance) obj;
		if (currency != other.currency)
			return false;
		if (market == null) {
			if (other.market != null)
				return false;
		} else if (!market.equals(other.market))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Balance [market=" + market + ", currency=" + currency + ", confirmedAmount=" + confirmedAmount
				+ ", workingAmount=" + workingAmount + "]";
	}
	
}
