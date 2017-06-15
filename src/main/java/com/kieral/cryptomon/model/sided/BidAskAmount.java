package com.kieral.cryptomon.model.sided;

import com.kieral.cryptomon.model.trading.TradeAmount;

public class BidAskAmount extends SidedValue<TradeAmount> {

	public BidAskAmount(TradeAmount bidAmount, TradeAmount askAmount) {
		super(bidAmount, askAmount);
	}

	@Override
	public String toString() {
		return "BidAskAmount [bidAmount=" + getBidValue() + ", askAmount=" + getAskValue() + "]";
	}

}
