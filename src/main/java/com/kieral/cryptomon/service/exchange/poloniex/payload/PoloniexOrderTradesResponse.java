package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.TradesResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderTradesResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrderResponse {

	private static final long serialVersionUID = -5575116744128597604L;

	private AtomicBoolean collated = new AtomicBoolean(false);
	private PoloniexFilledOrderResponse orderResponse;
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
		orderResponse = new PoloniexFilledOrderResponse(orderId, this.subList(0,  this.size()));
	}
	
	@Override
	public String getOrderId() {
		return orderId;
	}

	@Override
	public boolean isOpen() {
		collate();
		return orderResponse == null ? false : orderResponse.isOpen();
	}

	@Override
	public boolean isSuccess() {
		collate();
		return orderResponse == null ? false : orderResponse.isSuccess();
	}

	@Override
	public BigDecimal getAmount() {
		throw new IllegalStateException("Calling getAmount() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public BigDecimal getPrice() {
		throw new IllegalStateException("Calling getPrice() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public Side getSide() {
		throw new IllegalStateException("Calling getSide() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public long getCreatedTime() {
		throw new IllegalStateException("Calling getCreatedTime() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public long getClosedTime() {
		throw new IllegalStateException("Calling getClosedTime() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public TradesResponse getTradeResponses() {
		return orderResponse == null ? null : orderResponse.getTradeResponses();
	}

	@Override
	public OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder) {
		return orderResponse == null ? OrderStatus.ERROR : orderResponse.getOrderStatus(requestNature, originalOrder);
	}

	
}
