package com.kieral.cryptomon.service.rest;

import java.util.List;

public interface OrdersResponse<T extends OrderResponse> {

	boolean isSuccess();
	
	String getMessage();
	
	List<T> getOrderResponses();
	
}
