package com.kieral.cryptomon.model.sided;

import java.math.BigDecimal;

public class BidAskAmount extends SidedValue<BigDecimal> {

	public BidAskAmount(BigDecimal bidAmount, BigDecimal askAmount) {
		super(bidAmount, askAmount);
	}

	@Override
	public String toString() {
		return "BidAskAmount [bidAmount=" + getBidValue() + ", askAmount=" + getAskValue() + "]";
	}

}
