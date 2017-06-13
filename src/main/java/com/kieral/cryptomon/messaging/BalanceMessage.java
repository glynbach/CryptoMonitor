package com.kieral.cryptomon.messaging;

import java.util.List;

public class BalanceMessage {

	private String market;
	private List<BalanceEntryMessage> balances;
	
	public BalanceMessage(String market, List<BalanceEntryMessage> balances) {
		this.market = market;
		this.balances = balances;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public List<BalanceEntryMessage> getBalances() {
		return balances;
	}

	public void setBalances(List<BalanceEntryMessage> balances) {
		this.balances = balances;
	}

	@Override
	public String toString() {
		return "BalanceMessage [market=" + market + ", balances=" + balances + "]";
	}
	
}
