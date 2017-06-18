package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;
import com.kieral.cryptomon.service.util.TradingUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderTradesResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrderResponse {

	private static final long serialVersionUID = -5575116744128597604L;

	@Override
	public String toString() {
		return "PoloniexOrderTradesResponse [orderTrades()=" + super.toString() + "]";
	}

	// TODO: this still needs to be collated
	
	@Override
	public String getOrderId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getQuantity() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getQuantityRemaining() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BigDecimal getPrice() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isClosing() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSuccess() {
		return getOrderId() != null;
	}

	@Override
	public OrderStatus getOrderStatus() {
		return TradingUtils.getOrderStatus(this); 
	}

	@Override
	public Side getSide() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCreatedTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getClosedTime() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
