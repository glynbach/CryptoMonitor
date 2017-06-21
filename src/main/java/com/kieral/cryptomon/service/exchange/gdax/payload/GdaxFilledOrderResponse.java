package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.BaseTradesResponse;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.rest.TradeResponse;
import com.kieral.cryptomon.service.rest.TradesResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

public class GdaxFilledOrderResponse implements OrderResponse {

	private final String orderId;
	private final BaseTradesResponse tradesResponses;
	
	public GdaxFilledOrderResponse(String orderId, List<? extends TradeResponse> tradeResponses) {
		this.orderId = orderId;
		tradesResponses = new BaseTradesResponse(tradeResponses);
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public String getOrderId() {
		return orderId;
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
	public boolean isOpen() {
		return false;
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
		return tradesResponses;
	}

	@Override
	public OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder) {
		return TradingUtils.getOrderStatus(requestNature, isOpen(), originalOrder.getAmount(), tradesResponses);
	}

}
