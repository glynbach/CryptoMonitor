package com.kieral.cryptomon.service.exchange.poloniex.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.kieral.cryptomon.service.exchange.poloniex.PoloniexServiceConfig;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderTradeResponse;

public class PoloniexUtils {

	public static List<PoloniexOrderResponse> getOrderResponsesFromTrades(boolean openOrder, List<PoloniexOrderTradeResponse> trades) {
		List<PoloniexOrderResponse> responses = new ArrayList<PoloniexOrderResponse>();
		if (trades != null) {
			Map<String, List<PoloniexOrderTradeResponse>> orderTrades = new LinkedHashMap<String, List<PoloniexOrderTradeResponse>>();
			trades.forEach(trade -> {
				if (!orderTrades.containsKey(trade.getOrderNumber()))
					orderTrades.put(trade.getOrderNumber(), new ArrayList<PoloniexOrderTradeResponse>());
				orderTrades.get(trade.getOrderNumber()).add(trade);
			});
			orderTrades.keySet().forEach(orderId -> {
				responses.add(getOrderResponseFromTrades(openOrder, orderId, orderTrades.get(orderId)));
			});
		}
		return responses;
	}

	public static PoloniexOrderResponse getOrderResponseFromTrades(boolean openOrder, String orderId, List<PoloniexOrderTradeResponse> trades) {
		
		if (trades == null) {
			return null;
		}
		AtomicReference<String> date = new AtomicReference<String>();
		AtomicReference<BigDecimal> rate = new AtomicReference<BigDecimal>();
		AtomicReference<String> type = new AtomicReference<String>();
		AtomicReference<BigDecimal> amount = new AtomicReference<BigDecimal>(BigDecimal.ZERO);
		AtomicReference<BigDecimal> fee = new AtomicReference<BigDecimal>(BigDecimal.ZERO);
		trades.forEach(trade -> {
			date.compareAndSet(null, trade.getDate());
			rate.compareAndSet(null, trade.getRate());
			type.compareAndSet(null, trade.getType());
			if (trade.getAmount() != null) {
				amount.set(amount.get().add(trade.getAmount()));
			}
			if (trade.getFee() != null) {
				fee.set(fee.get().add(trade.getFee()));
			}
		});
		PoloniexOrderResponse response = new PoloniexOrderResponse();
		response.setOrderNumber(orderId);
		response.setRate(rate.get());
		response.setTotal(amount.get());
		response.setAmount(amount.get());
		response.setFee(fee.get());
		response.setType(type.get());
		if (!openOrder) {
			try {
				response.setClosedTime(LocalDateTime.parse(date.get(), PoloniexServiceConfig.dateTimeFormatter).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
			} catch (Exception e) {
				response.setClosedTime(System.currentTimeMillis());
			}
		} 
		return response;
	}

}
