package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OpenOrderStatus;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderResponse implements OrderResponse {

	private String orderNumber;
	private String type;
	private BigDecimal rate;
	private BigDecimal amount;
	private BigDecimal fee;
	private BigDecimal total;
	private long closedTime;
	
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

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	@Override
	public String toString() {
		return "PoloniexOrderResponse [orderNumber=" + orderNumber + ", type=" + type + ", rate=" + rate + ", amount="
				+ amount + ", fee=" + fee + ", total=" + total + ", closedTime=" + closedTime + "]";
	}

	@Override
	public String getOrderId() {
		return orderNumber;
	}

	@Override
	public BigDecimal getAmountRemaining() {
		return amount;
	}

	@Override
	public BigDecimal getPrice() {
		return rate;
	}

	@Override
	public boolean isOpen() {
		return closedTime == 0;
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
		return type == null ? null : type.toUpperCase().contains("SELL") ? Side.ASK : Side.BID;
	}

	@Override
	public long getCreatedTime() {
		return System.currentTimeMillis();
	}

	@Override
	public long getClosedTime() {
		return closedTime;
	}

	@Override
	public OpenOrderStatus getOrderUpdateStatus(boolean isOpenOrderRequest, Order order) {
		// Poloniex applies the fee to the base currency
		BigDecimal amountRemaining = order.getAmount().subtract(amount.add(fee == null ? BigDecimal.ZERO : fee));
		return new OpenOrderStatus(order, TradingUtils.getOrderStatus(isOpenOrderRequest, amountRemaining), amountRemaining);
	}

	public void setClosedTime(long closedTime) {
		this.closedTime = closedTime;
	}
	
}
