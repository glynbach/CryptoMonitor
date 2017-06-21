package com.kieral.cryptomon.service.exchange.gdax.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxFillResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxFilledOrderResponse;

public class GdaxUtils {

	public static List<GdaxFilledOrderResponse> getOrderResponsesFromFills(boolean openOrder, List<GdaxFillResponse> fills) {
		List<GdaxFilledOrderResponse> responses = new ArrayList<GdaxFilledOrderResponse>();
		if (fills != null) {
			Map<String, List<GdaxFillResponse>> orderFills = new LinkedHashMap<String, List<GdaxFillResponse>>();
			fills.forEach(fill -> {
				if (!orderFills.containsKey(fill.getOrderId()))
					orderFills.put(fill.getOrderId(), new ArrayList<GdaxFillResponse>());
				orderFills.get(fill.getOrderId()).add(fill);
			});
			orderFills.keySet().forEach(orderId -> {
				responses.add(new GdaxFilledOrderResponse(orderId, fills));
			});
		}
		return responses;
	}

}
