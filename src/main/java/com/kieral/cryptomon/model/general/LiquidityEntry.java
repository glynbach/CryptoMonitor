package com.kieral.cryptomon.model.general;

import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskPrice;

public class LiquidityEntry {

	private final BidAskPrice bidAskPrice;
	private final BidAskAmount bidAskAmount;
	
	public LiquidityEntry(BidAskPrice bidAskPrice, BidAskAmount bidAskAmount) {
		this.bidAskPrice = bidAskPrice;
		this.bidAskAmount = bidAskAmount;
	}

	public BidAskPrice getBidAskPrice() {
		return bidAskPrice;
	}

	public BidAskAmount getBidAskAmount() {
		return bidAskAmount;
	}

	@Override
	public String toString() {
		return "LiquidityEntry [bidAskPrice=" + bidAskPrice + ", bidAskAmount=" + bidAskAmount + "]";
	}
	
}
