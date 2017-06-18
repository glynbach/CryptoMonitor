package com.kieral.cryptomon.service;

import com.kieral.cryptomon.model.trading.Order;

public interface OrderListener {

	void onOrderStatusChange(Order order);
	
}
