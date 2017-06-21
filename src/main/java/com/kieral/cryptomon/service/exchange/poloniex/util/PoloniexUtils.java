package com.kieral.cryptomon.service.exchange.poloniex.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexFilledOrderResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderTradeResponse;

public class PoloniexUtils {

	public static List<PoloniexFilledOrderResponse> getOrderResponsesFromTrades(boolean openOrder, List<PoloniexOrderTradeResponse> trades) {
		List<PoloniexFilledOrderResponse> responses = new ArrayList<PoloniexFilledOrderResponse>();
		if (trades != null) {
			Map<String, List<PoloniexOrderTradeResponse>> orderTrades = new LinkedHashMap<String, List<PoloniexOrderTradeResponse>>();
			trades.forEach(trade -> {
				if (!orderTrades.containsKey(trade.getOrderNumber()))
					orderTrades.put(trade.getOrderNumber(), new ArrayList<PoloniexOrderTradeResponse>());
				orderTrades.get(trade.getOrderNumber()).add(trade);
			});
			orderTrades.keySet().forEach(orderId -> {
				responses.add(new PoloniexFilledOrderResponse(orderId, orderTrades.get(orderId)));
			});
		}
		return responses;
	}

}
