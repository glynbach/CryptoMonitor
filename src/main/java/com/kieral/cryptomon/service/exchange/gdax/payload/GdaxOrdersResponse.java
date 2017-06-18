package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxOrdersResponse extends ArrayList<GdaxOrderResponse> implements OrdersResponse<GdaxOrderResponse> {

	private static final long serialVersionUID = 7673148568278678031L;

	public List<GdaxOrderResponse> getOrderResponses() {
		return new ArrayList<GdaxOrderResponse>(this.subList(0, this.size()));
	}

	@Override
	public String toString() {
		return "GdaxOrdersResponse [orderResponses=" + getOrderResponses() + "]";
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public String getMessage() {
		return null;
	}
	
}
