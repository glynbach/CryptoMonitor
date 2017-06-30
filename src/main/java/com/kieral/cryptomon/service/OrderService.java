package com.kieral.cryptomon.service;

import java.util.List;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;

public interface OrderService {

	public void init();

	public boolean isTradingEnabled(String market);

	public void registerOrderListener(OrderListener listener);
	
	public List<Order> getClosedOrders(String market);

	public List<Order> getOpenOrders(String market);

	public Order getOrder(String market, String clientOrderId);

	public List<Order> getAllOrders(String market);
	
	public void placeOrder(Order order);
	
	public void cancelOrder(String market, String clientOrderId) throws OrderNotExistsException;

	public OrderStatus forceCancelOrder(String market, String orderId) throws OrderNotExistsException;

	public void checkStatus(String market, String clientOrderId) throws OrderNotExistsException;

}
