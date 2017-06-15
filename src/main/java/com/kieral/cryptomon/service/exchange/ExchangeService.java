package com.kieral.cryptomon.service.exchange;

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
	 * Returns true unless disabled in config 
	 */
	boolean isEnabled();

	/**
	 * Returns the name of this exchange 
	 */
	String getName();
	
	/**
	 * Returns true if trading has not yet been unlocked 
	 */
	boolean isTradingLocked();
	
	/**
	 * @param secretKey for the encrypted API keys
	 * @returns false if trading is unlocked
	 */
	boolean unlockTrading(String secretKey);
	
	/**
	 * Updates the current balances available for this exchange  
	 */
	void updateBalances(boolean overrideWorkingBalance) throws BalanceRequestException;

}
