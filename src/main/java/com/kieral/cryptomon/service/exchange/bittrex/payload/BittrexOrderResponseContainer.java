package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;

public class BittrexOrderResponseContainer extends BittrexResponseContainer<BittrexOrderResponse> implements OrderResponse {

	@Override
	public String getOrderId() {
		return this.getResult() == null ? null : this.getResult().getUuid();
	}

	@Override
	public BigDecimal getQuantity() {
		return this.getResult() == null ? null : this.getResult().getQuantity();
	}

	@Override
	public BigDecimal getQuantityRemaining() {
		return this.getResult() == null ? null : this.getResult().getQuantityRemaining();
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
	
}
