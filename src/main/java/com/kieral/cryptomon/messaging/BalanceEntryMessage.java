package com.kieral.cryptomon.messaging;

public class BalanceEntryMessage {

	private String currency;
	private String amount;
	
	public BalanceEntryMessage() {
	}
	
	public BalanceEntryMessage(String cutrency, String amount) {
		this.currency = cutrency;
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	@Override
	public String toString() {
		return "BalanceEntryMessage [currency=" + currency + ", amount=" + amount + "]";
	}
	
}
