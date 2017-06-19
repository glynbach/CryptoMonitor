package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;

public class TradingUtils {

	public static OrderStatus getOrderStatus(OrderResponse orderResponse) {
		if (orderResponse == null)
			return OrderStatus.ERROR;
		return getOrderStatus(orderResponse.isOpen(), orderResponse.getAmountRemaining()); 
	}

	public static OrderStatus getOrderStatus(boolean open, BigDecimal amountRemaining) {
		return open 
				? amountRemaining == null || amountRemaining.compareTo(BigDecimal.ZERO) <= 0 ? OrderStatus.PARTIALLY_FILLED : OrderStatus.OPEN
				: amountRemaining == null || amountRemaining.compareTo(BigDecimal.ZERO) <= 0 ? OrderStatus.FILLED : OrderStatus.CANCELLED; 
	}

}
