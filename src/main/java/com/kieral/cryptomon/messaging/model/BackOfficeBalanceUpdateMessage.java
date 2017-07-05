package com.kieral.cryptomon.messaging.model;

public class BackOfficeBalanceUpdateMessage {

	private String currency;
	private String originalAmount;
	private String currentAmount;
	private boolean increase;
	
	public BackOfficeBalanceUpdateMessage() {
	}
	
	public BackOfficeBalanceUpdateMessage(String currency, String originalAmount, String currentAmount,
			boolean increase) {
		this.currency = currency;
		this.originalAmount = originalAmount;
		this.currentAmount = currentAmount;
		this.increase = increase;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getOriginalAmount() {
		return originalAmount;
	}

	public void setOriginalAmount(String originalAmount) {
		this.originalAmount = originalAmount;
	}

	public String getCurrentAmount() {
		return currentAmount;
	}

	public void setCurrentAmount(String currentAmount) {
		this.currentAmount = currentAmount;
	}

	public boolean isIncrease() {
		return increase;
	}

	public void setIncrease(boolean increase) {
		this.increase = increase;
	}

	@Override
	public String toString() {
		return "BackOfficeBalanceUpdateMessage [currency=" + currency + ", originalAmount=" + originalAmount
				+ ", currentAmount=" + currentAmount + ", increase=" + increase + "]";
	}
	
}
