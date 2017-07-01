package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.trading.AmountValue;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.OrderResponse.RequestNature;
import com.kieral.cryptomon.service.rest.TradesResponse;

public class TradingUtils {

	private final static BigDecimal FILLED_DISREPANCY_TOLERANCE = new BigDecimal("0.001");
	
	public static OrderStatus getOrderStatus(RequestNature requestNature, boolean open, BigDecimal originalAmount,
							TradesResponse tradeResponses) {
		switch (requestNature) {
			case CLOSED_ORDER_RESPONSE:
				return getOrderStatusResponseForOpenStatus(false, originalAmount, tradeResponses);
			case OPEN_ORDER_RESPONSE:
				return getOrderStatusResponseForOpenStatus(true, originalAmount, tradeResponses);
			default:
				return getOrderStatusResponseForOpenStatus(open, originalAmount, tradeResponses);
		}
	}

	private static OrderStatus getOrderStatusResponseForOpenStatus(boolean open, BigDecimal originalAmount,
							TradesResponse tradeResponses) {
		if (!open) {
			if (tradeResponses == null || tradeResponses.getNumTrades() == 0)
				return OrderStatus.CANCELLED;
			if (originalAmount == null) {
				// we can't assess if it is filled if we don't know the order amount
				return OrderStatus.ERROR;
			}
			BigDecimal filledAmount = tradeResponses.getFilledAmount();
			if (filledAmount.compareTo(BigDecimal.ZERO) != 0 && originalAmount.subtract(filledAmount).compareTo(FILLED_DISREPANCY_TOLERANCE) < 0) {
				return OrderStatus.FILLED;
			}
			return OrderStatus.CANCELLED;
		} else {
			if (tradeResponses.getFilledAmount().compareTo(BigDecimal.ZERO) == 0)
				return OrderStatus.OPEN;
			else
				return OrderStatus.PARTIALLY_FILLED;
		}
	}
	
	public static BigDecimal getFilledAmount(List<? extends AmountValue> amountValues) {
		if (amountValues == null || amountValues.size() == 0)
			return BigDecimal.ZERO;
		BigDecimal filledAmount = BigDecimal.ZERO;
		for (AmountValue amountValue : amountValues) {
			if (amountValue.getAmount() != null) {
				filledAmount = filledAmount.add(amountValue.getAmount());
			}
		}
		return filledAmount;
	}

	public static BigDecimal getFilledAmount(Order... orders) {
		if (orders == null)
			return BigDecimal.ZERO;
		BigDecimal filledAmount = BigDecimal.ZERO;
		for (Order order : orders) {
			filledAmount = filledAmount.add(getFilledAmount(order.getTrades()));
		}
		return filledAmount;
	}

}
