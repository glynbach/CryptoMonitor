package com.kieral.cryptomon.model.trading;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TradeAmount {

	private final BigDecimal baseAmount;
	private final BigDecimal quotedAmount;
	
	public TradeAmount(BigDecimal baseAmount, BigDecimal quotedAmount) {
		this.baseAmount = baseAmount;
		this.quotedAmount = quotedAmount;
	}

	public TradeAmount(BigDecimal baseAmount, BigDecimal price, int scale) {
		this.baseAmount = baseAmount;
		this.quotedAmount = baseAmount == null || price == null ? BigDecimal.ZERO : 
			baseAmount.multiply(price).setScale(scale, RoundingMode.HALF_DOWN);
	}

	public BigDecimal getBaseAmount() {
		return baseAmount;
	}

	public BigDecimal getQuotedAmount() {
		return quotedAmount;
	}

	@Override
	public String toString() {
		return "TradeAmount [baseAmount=" + baseAmount + ", quotedAmount=" + quotedAmount + "]";
	}
	
}
