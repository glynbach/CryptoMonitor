package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrderHistoryResponse extends ArrayList<PoloniexOrderTradeResponse> implements OrdersResponse {

	private static final long serialVersionUID = 2072663639186818275L;

	@Override
	public String toString() {
		return "PoloniexOrderHistoryResponse [orderTrades()=" + super.toString() + "]";
	}

	
}
