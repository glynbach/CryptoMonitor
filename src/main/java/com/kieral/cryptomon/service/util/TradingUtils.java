package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse;

public class TradingUtils {

	public static boolean isPartiallyFilled(BigDecimal quantity, BigDecimal remainingQuantity) {
		if (quantity == null)
			return false;
		if (remainingQuantity == null)
			return false;
		return (quantity.compareTo(remainingQuantity) != 0);
	}

	public static OrderStatus getOrderStatus(OrderResponse orderResponse) {
		if (orderResponse == null)
			return OrderStatus.ERROR;
		return orderResponse.isOpen() 
			? isPartiallyFilled(orderResponse.getQuantity(), orderResponse.getQuantityRemaining()) ? OrderStatus.PARTIALLY_FILLED : OrderStatus.OPEN
			: orderResponse.getQuantityRemaining() == null || orderResponse.getQuantityRemaining().compareTo(BigDecimal.ZERO) == 0 ? OrderStatus.FILLED : OrderStatus.CANCELLED; 
	}

}
