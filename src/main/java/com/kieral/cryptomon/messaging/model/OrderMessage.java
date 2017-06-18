package com.kieral.cryptomon.messaging.model;

import java.time.Instant;
import java.time.ZoneId;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.service.util.CommonUtils;

public class OrderMessage {

	private String market;
	private String currencyPair;
	private String amount;
	private String price;
	private String side;
	private String orderStatus;
	private String clientOrderId;
	private String orderId;
	private String createdTime;
	private String closedTime;
	private String message;
	
	public OrderMessage() {
	}

	public OrderMessage(Order order) {
		if (order == null)
			return;
		market = order.getMarket();
		currencyPair = order.getCurrencyPair() == null ? order.getCurrencyPairStr() : order.getCurrencyPair().getName();
		amount = order.getAmount() == null ? "0.00" : order.getAmount().toPlainString();
		price = order.getPrice() == null ? "0.00" : order.getPrice().toPlainString();
		side = order.getSide() == null ? "?" : order.getSide().name().toLowerCase();
		orderStatus = order.getOrderStatus() == null ? "?" : order.getOrderStatus().name().toLowerCase();
		clientOrderId = order.getClientOrderId();
		orderId = order.getOrderId();
		createdTime = order.getCreatedTime() > 0 ? CommonUtils.SECONDS_FORMATTER.format(Instant.ofEpochMilli(order.getCreatedTime())
				.atZone(ZoneId.systemDefault()).toLocalDateTime()) : "";
		closedTime = order.getClosedTime() > 0 ? CommonUtils.SECONDS_FORMATTER.format(Instant.ofEpochMilli(order.getClosedTime())
				.atZone(ZoneId.systemDefault()).toLocalDateTime()) : "";
		message = order.getMessage();
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}

	public void setCurrencyPair(String currencyPair) {
		this.currencyPair = currencyPair;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getClientOrderId() {
		return clientOrderId;
	}

	public void setClientOrderId(String clientOrderId) {
		this.clientOrderId = clientOrderId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(String createdTime) {
		this.createdTime = createdTime;
	}

	public String getClosedTime() {
		return closedTime;
	}

	public void setClosedTime(String closedTime) {
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
		return "OrderMessage [market=" + market + ", currencyPair=" + currencyPair + ", amount=" + amount + ", price="
				+ price + ", side=" + side + ", orderStatus=" + orderStatus + ", clientOrderId=" + clientOrderId
				+ ", orderId=" + orderId + ", createdTime=" + createdTime + ", closedTime=" + closedTime + ", message="
				+ message + "]";
	}
	
}
