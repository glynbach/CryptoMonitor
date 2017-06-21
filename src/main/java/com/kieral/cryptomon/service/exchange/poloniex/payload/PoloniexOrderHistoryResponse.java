package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.exchange.poloniex.util.PoloniexUtils;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderHistoryResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrdersResponse<PoloniexFilledOrderResponse> {

	private static final long serialVersionUID = 2072663639186818275L;

	private AtomicBoolean collated = new AtomicBoolean(false);
	private List<PoloniexFilledOrderResponse> responses;
			
	@Override
	public String toString() {
		return "PoloniexOrderHistoryResponse [orderTrades()=" + super.toString() + "]";
	}

	private void collate() {
		if (collated.compareAndSet(false,  true)) {
			doCollate();
		}
	}

	private void doCollate() {
		collated.set(true);
		responses = PoloniexUtils.getOrderResponsesFromTrades(false, this.subList(0,  this.size()));
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public String getMessage() {
		return null;
	}

	@Override
	public List<PoloniexFilledOrderResponse> getOrderResponses() {
		collate();
		return responses;
	}

}
