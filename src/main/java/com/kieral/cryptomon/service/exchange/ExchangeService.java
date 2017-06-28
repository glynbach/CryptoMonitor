package com.kieral.cryptomon.service.exchange;

import java.util.List;
import java.util.Map;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatusListener;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.liquidity.OrderBookListener;

public interface ExchangeService {

	/**
	 * Connects to any persistent streams and services if required 
	 */
	void connect() throws InterruptedException;
	
	/**
	 * Disconnects from any streams or services subscribed to 
	 */
	void disconnect();
	
	/**
	 * Adds an order book listener to this exchange 
	 */
	void registerOrderBookListener(OrderBookListener orderBookListener);

	/**
	 * Adds a connection status listener to this exchange 
	 */
	void registerStatusListener(ConnectionStatusListener statusListener);

	/**
	 * Adds a trading status listener to this exchange 
	 */
	void registerTradingStatusListener(TradingStatusListener tradingStatusListener);

	/**
	 * Returns true unless disabled in config 
	 */
	boolean isEnabled();

	/**
	 * Returns the name of this exchange 
	 */
	String getName();
	
	/**
	 * Returns true if trading is enabled
	 */
	boolean isTradingEnabled();
	
	/**
	 * @param secretKey for the encrypted API keys
	 * @returns true if trading successfully enabled
	 */
	boolean enableTrading(String secretKey);
	
	/**
	 * Updates the current balances available for this exchange  
	 */
	void updateBalances(boolean overrideWorkingBalance) throws BalanceRequestException;

	/**
	 * Returns a valid currency pair from the given string; format XXXYYY (base/quoted)
	 */
	CurrencyPair getCurrencyPair(String currencyPairStr);

	/**
	 * Places a limit order on the exchange returning the resulting status (CANCELLED if failed to place the order)
	 */
	OrderStatus placeOrder(Order order);

	/**
	 * Cancels a known limit order on the exchange returning the resulting status or existing status if failed to cancel
	 */
	OrderStatus cancelOrder(Order order);

	/**
	 * Cancels an unknown limit order on the exchange returning the resulting status
	 */
	OrderStatus cancelOrder(String orderId);

	/**
	 * Returns a map of order statuses for the given orders keye by clientOrderId
	 */
	Map<String, OrderStatus> getOpenOrderStatuses(List<Order> orders);

	/**
	 * Returns a list of all open orders
	 */
	List<Order> getOpenOrders();

	/**
	 * Returns the ServiceExchangeProperties
	 */
	ServiceExchangeProperties getProperties();

}
