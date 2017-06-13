package com.kieral.cryptomon.model.sided;

import java.math.BigDecimal;

public class BidAskPrice extends SidedValue<BigDecimal> {

	public BidAskPrice(BigDecimal bid, BigDecimal ask) {
		super(bid, ask);
	}

	@Override
	public String toString() {
		return "BidAskPrice [bid=" + getBidValue() + ", ask=" + getAskValue() + "]";
	}
	
}
