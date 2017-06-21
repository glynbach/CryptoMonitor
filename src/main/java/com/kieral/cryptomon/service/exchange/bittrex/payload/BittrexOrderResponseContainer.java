package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.TradesResponse;

public class BittrexOrderResponseContainer extends BittrexResponseContainer<BittrexOrderResponse> implements OrderResponse {

	@Override
	public String getOrderId() {
		return this.getResult() == null ? null : this.getResult().getOrderId();
	}

	@Override
	public BigDecimal getAmount() {
		return this.getResult() == null ? null : this.getResult().getAmount();
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
	public Side getSide() {
		return this.getResult() == null ? null : this.getResult().getSide();
	}

	@Override
	public long getCreatedTime() {
		return this.getResult() == null ? 0 : this.getResult().getCreatedTime();
	}

	@Override
	public long getClosedTime() {
		return this.getResult() == null ? 0 : this.getResult().getClosedTime();
	}

	@Override
	public TradesResponse getTradeResponses() {
		return this.getResult() == null ? null : this.getResult().getTradeResponses();
	}

	@Override
	public OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder) {
		return this.getResult() == null ? null : this.getResult().getOrderStatus(requestNature, originalOrder);
	}
	
}
