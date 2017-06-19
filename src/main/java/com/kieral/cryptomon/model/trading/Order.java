package com.kieral.cryptomon.model.trading;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;

public class Order {

	private String market;
	private String currencyPairStr;
	private CurrencyPair currencyPair;
	private BigDecimal amount;
	private BigDecimal amountRemaining;
	private BigDecimal price;
	private Side side;
	private OrderStatus orderStatus;
	private String clientOrderId;
	private String orderId;
	private long createdTime;
	private long closedTime;
	private String message;
	
	public Order() {
		amount = new BigDecimal("0.00");
		price = new BigDecimal("0.00");
		side = Side.BID;
	}

	public Order(String market, String currencyPairStr, BigDecimal amount, BigDecimal price, Side side) {
		this.market = market;
		this.currencyPairStr = currencyPairStr;
		this.amount = amount;
		this.amountRemaining = amount;
		this.price = price;
		this.side = side;
	}

	public Order(String market, String currencyPairStr, CurrencyPair currencyPair, BigDecimal amount, BigDecimal amountRemaining, BigDecimal price,
			Side side, OrderStatus orderStatus, String clientOrderId, String orderId, long createdTime, long closedTime,
			String message) {
		this.market = market;
		this.currencyPairStr = currencyPairStr;
		this.currencyPair = currencyPair;
		this.amount = amount;
		this.amountRemaining = amountRemaining;
		this.price = price;
		this.side = side;
		this.orderStatus = orderStatus;
		this.clientOrderId = clientOrderId;
		this.orderId = orderId;
		this.createdTime = createdTime;
		this.closedTime = closedTime;
		this.message = message;
	}

//	public Order(Order order) {
//		this.market = order.getMarket();
//		this.currencyPairStr = order.getCurrencyPairStr();
//		this.currencyPair = order.getCurrencyPair();
//		this.amount = order.getAmount();
//		this.price = order.getPrice();
//		this.side = order.getSide();
//		this.orderStatus = order.getOrderStatus();
//		this.clientOrderId = order.getClientOrderId();
//		this.orderId = order.getOrderId();
//		this.createdTime = order.getCreatedTime();
//		this.closedTime = order.getClosedTime();
//		this.message = order.getMessage();
//	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getCurrencyPairStr() {
		return currencyPairStr;
	}

	public void setCurrencyPairStr(String currencyPairStr) {
		this.currencyPairStr = currencyPairStr;
	}

	public CurrencyPair getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(CurrencyPair currencyPair) {
		this.currencyPair = currencyPair;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getAmountRemaining() {
		return amountRemaining;
	}

	public void setAmountRemaining(BigDecimal amountRemaining) {
		this.amountRemaining = amountRemaining;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Side getSide() {
		return side;
	}

	public void setSide(Side side) {
		this.side = side;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(OrderStatus orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public long getCreatedTime() {
		return createdTime;
	}

	public String getClientOrderId() {
		return clientOrderId;
	}

	public void setClientOrderId(String clientOrderId) {
		this.clientOrderId = clientOrderId;
	}

	public void setCreatedTime(long createdTime) {
		this.createdTime = createdTime;
	}

	public long getClosedTime() {
		return closedTime;
	}

	public void setClosedTime(long closedTime) {
		this.closedTime = closedTime;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "Order [market=" + market + ", currencyPairStr=" + currencyPairStr + ", currencyPair=" + currencyPair
				+ ", amount=" + amount + ", amountRemaining=" + amountRemaining + ", price=" + price + ", side=" + side
				+ ", orderStatus=" + orderStatus + ", clientOrderId=" + clientOrderId + ", orderId=" + orderId
				+ ", createdTime=" + createdTime + ", closedTime=" + closedTime + ", message=" + message + "]";
	}

}
