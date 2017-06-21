package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexServiceConfig;
import com.kieral.cryptomon.service.rest.TradeResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderTradeResponse implements TradeResponse {

	private String globalTradeId;
	private String tradeId;
	private String currencyPair;
	private String date;
	private BigDecimal rate;
	private BigDecimal amount;
	private BigDecimal total;
	private BigDecimal fee;
	private String orderNumber;
	private String type;
	private String category;
	
	public String getGlobalTradeId() {
		return globalTradeId;
	}

	@JsonProperty("globalTradeID")
	public void setGlobalTradeId(String globalTradeId) {
		this.globalTradeId = globalTradeId;
	}

	public String getTradeId() {
		return tradeId;
	}

	@JsonProperty("tradeID")
	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(String currencyPair) {
		this.currencyPair = currencyPair;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public String toString() {
		return "PoloniexOrderTradeResponse [globalTradeId=" + globalTradeId + ", tradeId=" + tradeId + ", currencyPair="
				+ currencyPair + ", date=" + date + ", rate=" + rate + ", amount=" + amount + ", total=" + total
				+ ", fee=" + fee + ", orderNumber=" + orderNumber + ", type=" + type + ", category=" + category + "]";
	}

	@Override
	public long getTradeTime() {
		return CommonUtils.getMillis(date, PoloniexServiceConfig.dateTimeFormatter, 0);
	}

	@Override
	public boolean isFeeInQuotedCurrency() {
		return false;
	}
	
}
