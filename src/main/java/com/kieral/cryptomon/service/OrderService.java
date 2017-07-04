package com.kieral.cryptomon.service;

import java.util.List;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;

public interface OrderService {

	void init();

	boolean isTradingEnabled(String market);

	void registerOrderListener(OrderListener listener);
	
	List<Order> getClosedOrders(String market);

	List<Order> getOpenOrders(String market);

	Order getOrder(String market, String clientOrderId);

	List<Order> getAllOrders(String market);
	
	OrderStatus placeOrder(Order order);

	OrderStatus placeMarketOrder(Order order);

	OrderStatus cancelOrder(String market, String clientOrderId) throws OrderNotExistsException;

	OrderStatus forceCancelOrder(String market, String orderId) throws OrderNotExistsException;

	void checkStatus(String market, String clientOrderId) throws OrderNotExistsException;

	void checkStatuses(String market, List<Order> orders);
	
	void requestBalances();
	
}
