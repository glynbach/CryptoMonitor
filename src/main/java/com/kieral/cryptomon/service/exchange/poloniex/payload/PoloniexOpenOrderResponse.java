package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.BaseTradeResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.PlaceholderTradesResponse;
import com.kieral.cryptomon.service.rest.TradesResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOpenOrderResponse implements OrderResponse {

	private String orderNumber;
	private String type;
	private BigDecimal rate;
	private BigDecimal startingAmount;
	private BigDecimal amount;
	private BigDecimal total;
	
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

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public BigDecimal getStartingAmount() {
		return startingAmount;
	}

	public void setStartingAmount(BigDecimal startingAmount) {
		this.startingAmount = startingAmount;
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

	@Override
	public String getOrderId() {
		return orderNumber;
	}

	@Override
	public BigDecimal getPrice() {
		return rate;
	}

	@Override
	public boolean isOpen() {
		// use this class only for open orders
		return true;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}
	
	@Override
	public Side getSide() {
		return type == null ? null : type.toUpperCase().contains("SELL") ? Side.ASK : Side.BID;
	}

	@Override
	public long getCreatedTime() {
		return System.currentTimeMillis();
	}

	@Override
	public TradesResponse getTradeResponses() {
		if (amount != null && startingAmount != null && amount.compareTo(startingAmount) != 0) 
			return new PlaceholderTradesResponse(new BaseTradeResponse(null, getPrice(), getAmount(), BigDecimal.ZERO, 
					System.currentTimeMillis(), false));
		else
			return new PlaceholderTradesResponse(null);
	}

	@Override
	public OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder) {
		return TradingUtils.getOrderStatus(requestNature, isOpen(), originalOrder.getAmount(), getTradeResponses());
	}

	@Override
	public String toString() {
		return "PoloniexOrderResponse [orderNumber=" + orderNumber + ", type=" + type + ", rate=" + rate
				+ ", startingAmount=" + startingAmount + ", amount=" + amount + ", total=" + total + "]";
	}

	@Override
	public long getClosedTime() {
		return 0;
	}
	
}
