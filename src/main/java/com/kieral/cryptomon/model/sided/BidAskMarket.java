package com.kieral.cryptomon.model.sided;

public class BidAskMarket extends SidedValue<String> {

	public BidAskMarket(String bidMarket, String askMarket) {
		super(bidMarket, askMarket);
	}

	@Override
	public String toString() {
		return "BidAskMarket [bidMarket=" + getBidValue() + ", askMarket=" + getAskValue() + "]";
	}
	
}
