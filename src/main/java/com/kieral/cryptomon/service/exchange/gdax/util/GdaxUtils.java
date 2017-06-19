package com.kieral.cryptomon.service.exchange.gdax.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxFillResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxOrderResponse;

public class GdaxUtils {

	public static List<GdaxOrderResponse> getOrderResponsesFromFills(boolean openOrder, List<GdaxFillResponse> fills) {
		List<GdaxOrderResponse> responses = new ArrayList<GdaxOrderResponse>();
		if (fills != null) {
			Map<String, List<GdaxFillResponse>> orderFills = new LinkedHashMap<String, List<GdaxFillResponse>>();
			fills.forEach(fill -> {
				if (!orderFills.containsKey(fill.getOrderId()))
					orderFills.put(fill.getOrderId(), new ArrayList<GdaxFillResponse>());
				orderFills.get(fill.getOrderId()).add(fill);
			});
			orderFills.keySet().forEach(orderId -> {
				responses.add(getOrderResponseFromFills(openOrder, orderId, orderFills.get(orderId)));
			});
		}
		return responses;
	}
	
	public static GdaxOrderResponse getOrderResponseFromFills(boolean openOrder, String orderId, List<GdaxFillResponse> fills) {
		if (fills == null)
			return null;
		GdaxOrderResponse response = new GdaxOrderResponse();
		AtomicReference<String> doneAt = new AtomicReference<String>();
		AtomicReference<BigDecimal> price = new AtomicReference<BigDecimal>();
		AtomicReference<String> productId = new AtomicReference<String>();
		AtomicReference<String> side = new AtomicReference<String>();
		AtomicReference<BigDecimal> filledSize = new AtomicReference<BigDecimal>(BigDecimal.ZERO);
		fills.forEach(fill -> {
			doneAt.compareAndSet(null, fill.getCreatedAt());
			price.compareAndSet(null, fill.getPrice());
			side.compareAndSet(null, fill.getSide());
			productId.compareAndSet(null, fill.getProductId());
			if (fill.getSize() != null) {
				filledSize.set(filledSize.get().add(fill.getSize()));
			}
		});
		response.setId(orderId);
		response.setDoneAt(doneAt.get());
		response.setFilledSize(filledSize.get());
		response.setPrice(price.get());
		response.setProductId(productId.get());
		response.setSide(side.get());
		response.setStatus(openOrder ? "open" : "done");
		return response;
	}

}
