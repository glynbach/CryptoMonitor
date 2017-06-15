package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxAccountResponse;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexOrdersResponse extends ArrayList<GdaxAccountResponse> implements OrdersResponse {

	private static final long serialVersionUID = -291446473018656424L;

	@Override
	public String toString() {
		return "PoloniexOrdersResponse [orders()=" + super.toString() + "]";
	}

}
