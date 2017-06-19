package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OpenOrderStatus;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;

public class BittrexOrderResponseContainer extends BittrexResponseContainer<BittrexOrderResponse> implements OrderResponse {

	@Override
	public String getOrderId() {
		return this.getResult() == null ? null : this.getResult().getUuid();
	}

	@Override
	public BigDecimal getAmount() {
		return this.getResult() == null ? null : this.getResult().getAmount();
	}

	@Override
	public BigDecimal getAmountRemaining() {
		return this.getResult() == null ? null : this.getResult().getAmountRemaining();
	}

	@Override
	public BigDecimal getPrice() {
		return this.getResult() == null ? null : this.getResult().getPrice();
	}

	@Override
	public boolean isOpen() {
		return this.getResult() == null ? false : this.getResult().isOpen();
	}

	@Override
	public boolean isClosing() {
		return this.getResult() == null ? false : this.getResult().isCancelInitiated();
	}

	@Override
	public OrderStatus getOrderStatus() {
		return this.getResult() == null ? null : this.getResult().getOrderStatus();
	}

	@Override
	public Side getSide() {
		return this.getResult() == null ? null : this.getResult().getSide();
	}

	@Override
	public long getCreatedTime() {
		return this.getResult() == null ? 0 : this.getCreatedTime();
	}

	@Override
	public long getClosedTime() {
		return this.getResult() == null ? 0 : this.getClosedTime();
	}

	@Override
	public OpenOrderStatus getOrderUpdateStatus(boolean isOpenOrderRequest, Order order) {
		return this.getResult() == null ? null : this.getResult().getOrderUpdateStatus(isOpenOrderRequest, order);
	}

	
}
