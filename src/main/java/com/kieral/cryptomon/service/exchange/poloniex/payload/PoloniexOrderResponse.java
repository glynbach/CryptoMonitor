package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderResponse implements OrderResponse {

	private String orderNumber;
	private String type;
	private BigDecimal rate;
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
	public String toString() {
		return "PoloniexOrderResponse [orderNumber=" + orderNumber + ", type=" + type + ", rate=" + rate + ", amount="
				+ amount + ", total=" + total + "]";
	}

	@Override
	public String getOrderId() {
		return orderNumber;
	}

	@Override
	public BigDecimal getQuantity() {
		return amount;
	}

	@Override
	public BigDecimal getQuantityRemaining() {
		return amount;
	}

	@Override
	public BigDecimal getPrice() {
		return rate;
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isClosing() {
		return false;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}
	
	@Override
	public OrderStatus getOrderStatus() {
		return TradingUtils.getOrderStatus(this); 
	}

	@Override
	public Side getSide() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCreatedTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getClosedTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	
}
