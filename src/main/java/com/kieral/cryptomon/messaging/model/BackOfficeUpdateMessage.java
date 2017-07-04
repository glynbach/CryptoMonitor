package com.kieral.cryptomon.messaging.model;

import java.util.List;

public class BackOfficeUpdateMessage {

	private String currencyPair;
	private String longMarket;
	private int numLongTrades;
	private String longAmountTraded;
	private String longQuotedTraded;
	private String longCurrency;
	private String longPrice;
	private String shortMarket;
	private int numShortTrades;
	private String shortAmountTraded;
	private String shortQuotedTraded;
	private String shortCurrency;
	private String shortPrice;
	private List<BackOfficeBalanceUpdateMessage> balanceUpdates;
	
	public BackOfficeUpdateMessage() {
	}
	
	public BackOfficeUpdateMessage(String currencyPair, String longMarket, int numLongTrades, String longAmountTraded,
			String longQuotedTraded, String longCurrency, String longPrice, String shortMarket, int numShortTrades,
			String shortAmountTraded, String shortQuotedTraded, String shortCurrency, String shortPrice,
			List<BackOfficeBalanceUpdateMessage> balanceUpdates) {
		this.currencyPair = currencyPair;
		this.longMarket = longMarket;
		this.numLongTrades = numLongTrades;
		this.longAmountTraded = longAmountTraded;
		this.longQuotedTraded = longQuotedTraded;
		this.longCurrency = longCurrency;
		this.longPrice = longPrice;
		this.shortMarket = shortMarket;
		this.numShortTrades = numShortTrades;
		this.shortAmountTraded = shortAmountTraded;
		this.shortQuotedTraded = shortQuotedTraded;
		this.shortCurrency = shortCurrency;
		this.shortPrice = shortPrice;
		this.balanceUpdates = balanceUpdates;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(String currencyPair) {
		this.currencyPair = currencyPair;
	}

	public String getLongMarket() {
		return longMarket;
	}

	public void setLongMarket(String longMarket) {
		this.longMarket = longMarket;
	}

	public int getNumLongTrades() {
		return numLongTrades;
	}

	public void setNumLongTrades(int numLongTrades) {
		this.numLongTrades = numLongTrades;
	}

	public String getLongAmountTraded() {
		return longAmountTraded;
	}

	public void setLongAmountTraded(String longAmountTraded) {
		this.longAmountTraded = longAmountTraded;
	}

	public String getLongQuotedTraded() {
		return longQuotedTraded;
	}

	public void setLongQuotedTraded(String longQuotedTraded) {
		this.longQuotedTraded = longQuotedTraded;
	}

	public String getLongCurrency() {
		return longCurrency;
	}

	public void setLongCurrency(String longCurrency) {
		this.longCurrency = longCurrency;
	}

	public String getLongPrice() {
		return longPrice;
	}

	public void setLongPrice(String longPrice) {
		this.longPrice = longPrice;
	}

	public String getShortMarket() {
		return shortMarket;
	}

	public void setShortMarket(String shortMarket) {
		this.shortMarket = shortMarket;
	}

	public int getNumShortTrades() {
		return numShortTrades;
	}

	public void setNumShortTrades(int numShortTrades) {
		this.numShortTrades = numShortTrades;
	}

	public String getShortAmountTraded() {
		return shortAmountTraded;
	}

	public void setShortAmountTraded(String shortAmountTraded) {
		this.shortAmountTraded = shortAmountTraded;
	}

	public String getShortQuotedTraded() {
		return shortQuotedTraded;
	}

	public void setShortQuotedTraded(String shortQuotedTraded) {
		this.shortQuotedTraded = shortQuotedTraded;
	}

	public String getShortCurrency() {
		return shortCurrency;
	}

	public void setShortCurrency(String shortCurrency) {
		this.shortCurrency = shortCurrency;
	}

	public String getShortPrice() {
		return shortPrice;
	}

	public void setShortPrice(String shortPrice) {
		this.shortPrice = shortPrice;
	}

	public List<BackOfficeBalanceUpdateMessage> getBalanceUpdates() {
		return balanceUpdates;
	}

	public void setBalanceUpdates(List<BackOfficeBalanceUpdateMessage> balanceUpdates) {
		this.balanceUpdates = balanceUpdates;
	}

	@Override
	public String toString() {
		return "BackOfficeUpdateMessage [currencyPair=" + currencyPair + ", longMarket=" + longMarket
				+ ", numLongTrades=" + numLongTrades + ", longAmountTraded=" + longAmountTraded + ", longQuotedTraded="
				+ longQuotedTraded + ", longCurrency=" + longCurrency + ", longPrice=" + longPrice + ", shortMarket="
				+ shortMarket + ", numShortTrades=" + numShortTrades + ", shortAmountTraded=" + shortAmountTraded
				+ ", shortQuotedTraded=" + shortQuotedTraded + ", shortCurrency=" + shortCurrency + ", shortPrice="
				+ shortPrice + ", balanceUpdates=" + balanceUpdates + "]";
	}
	
}
