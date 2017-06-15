package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.service.rest.OrdersResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BittrexOrdersResponse implements OrdersResponse {

	private boolean success;
	private String message;
	private List<BittrexOrderResponse> openOrders;
	
	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<BittrexOrderResponse> getOpenOrders() {
		return openOrders;
	}

	public void setOpenOrders(List<BittrexOrderResponse> openOrders) {
		this.openOrders = openOrders;
	}

	@Override
	public String toString() {
		return "BittrexOpenOrdersResponse [success=" + success + ", message=" + message + ", openOrders=" + openOrders
				+ "]";
	}
	
}
