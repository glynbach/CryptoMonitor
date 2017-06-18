package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxFillsResponse extends ArrayList<GdaxFillResponse> implements OrdersResponse<GdaxOrderResponse> {

	private static final long serialVersionUID = 8269060090725506020L;

	public List<GdaxOrderResponse> getOrderResponses() {
		this.forEach(fill -> {
			// TODO: create an order history from the fills
		});
		return null;
	}

	@Override
	public String toString() {
		return "GdaxFillsResponse [getOrderResponses()=" + getOrderResponses() + ", fills=" + super.toString()
				+ "]";
	}

	@Override
	public boolean isSuccess() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
