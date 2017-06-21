package com.kieral.cryptomon.service.exchange.poloniex.payload;

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

public class PoloniexFilledOrderResponse implements OrderResponse {

	private final String orderId;
	private final TradesResponse tradesResponse;
	
	public PoloniexFilledOrderResponse(String orderId, List<? extends TradeResponse> trades) {
		this.orderId = orderId;
		tradesResponse = new BaseTradesResponse(trades);
	}
	
	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public TradesResponse getTradeResponses() {
		return tradesResponse;
	}

	@Override
	public OrderStatus getOrderStatus(RequestNature requestNature, Order originalOrder) {
		return TradingUtils.getOrderStatus(requestNature, isOpen(), originalOrder.getAmount(), tradesResponse);
	}

	@Override
	public String toString() {
		return "PoloniexFilledOrderResponse [tradesResponse=" + tradesResponse + ", toString()=" + super.toString() + "]";
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
	public long getCreatedTime() {
		throw new IllegalStateException("Calling getCreatedTime() is not permitted on " + this.getClass().getSimpleName());
	}

	@Override
	public long getClosedTime() {
		throw new IllegalStateException("Calling getClosedTime() is not permitted on " + this.getClass().getSimpleName());
	}

}
