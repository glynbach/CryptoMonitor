package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrderResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderTradesResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrderResponse {

	private static final long serialVersionUID = -5575116744128597604L;

	@Override
	public String toString() {
		return "PoloniexOrderTradesResponse [orderTrades()=" + super.toString() + "]";
	}

	
}
