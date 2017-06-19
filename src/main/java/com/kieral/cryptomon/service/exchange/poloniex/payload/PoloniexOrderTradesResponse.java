package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OpenOrderStatus;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exchange.poloniex.util.PoloniexUtils;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderTradesResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrderResponse {

	private static final long serialVersionUID = -5575116744128597604L;

	private AtomicBoolean collated = new AtomicBoolean(false);
	private PoloniexOrderResponse orderResponse;
	private String orderId = "unknown";
			
	@Override
	public String toString() {
		return "PoloniexOrderTradesResponse [orderTrades()=" + super.toString() + "]";
	}

	private void collate() {
		if (collated.compareAndSet(false,  true)) {
			doCollate(true);
		}
	}
	
	private void doCollate(boolean openOrder) {
		collated.set(true);
		orderResponse = PoloniexUtils.getOrderResponseFromTrades(openOrder, orderId, this.subList(0,  this.size()));
	}
	
	@Override
	public String getOrderId() {
		return orderId;
	}

	@Override
	public BigDecimal getAmount() {
		collate();
		return orderResponse == null ? null : orderResponse.getAmount();
	}

	@Override
	public BigDecimal getAmountRemaining() {
		collate();
		return orderResponse == null ? null : orderResponse.getAmountRemaining();
	}

	@Override
	public BigDecimal getPrice() {
		collate();
		return orderResponse == null ? null : orderResponse.getPrice();
	}

	@Override
	public boolean isOpen() {
		collate();
		return orderResponse == null ? false : orderResponse.isOpen();
	}

	@Override
	public boolean isClosing() {
		return orderResponse == null ? false : orderResponse.isClosing();
	}

	@Override
	public boolean isSuccess() {
		collate();
		return orderResponse == null ? false : orderResponse.isClosing();
	}

	@Override
	public OrderStatus getOrderStatus() {
		collate();
		throw new IllegalStateException("getOrderStatus not supported; use getOrderUpdateStatus(boolean isOpenOrderRequest, Order order)");
	}

	@Override
	public Side getSide() {
		collate();
		return orderResponse == null || orderResponse.getType() == null ? null : orderResponse.getType().toUpperCase().contains("BUY") ? Side.BID : Side.ASK;
	}

	@Override
	public long getCreatedTime() {
		collate();
		return orderResponse == null ? System.currentTimeMillis() : orderResponse.getCreatedTime();
	}

	@Override
	public long getClosedTime() {
		collate();
		return orderResponse == null ? System.currentTimeMillis() : orderResponse.getClosedTime();
	}

	@Override
	public OpenOrderStatus getOrderUpdateStatus(boolean isOpenOrderRequest, Order order) {
		if (order != null)
			orderId = order.getOrderId();
		doCollate(isOpenOrderRequest);
		BigDecimal amountRemaining = order.getAmount().subtract(getAmount().add(orderResponse == null || orderResponse.getFee() == null ? BigDecimal.ZERO : orderResponse.getFee()));
		return new OpenOrderStatus(order, TradingUtils.getOrderStatus(isOpenOrderRequest, amountRemaining), amountRemaining);
	}
	
}
