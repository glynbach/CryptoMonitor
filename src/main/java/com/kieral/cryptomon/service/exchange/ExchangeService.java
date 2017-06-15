package com.kieral.cryptomon.service.exchange;

import com.kieral.cryptomon.service.connection.ConnectionStatusListener;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.liquidity.OrderBookListener;

public interface ExchangeService {

	void connect() throws InterruptedException;
	
	void disconnect();
	
	void registerOrderBookListener(OrderBookListener orderBookListener);
	
	void registerStatusListener(ConnectionStatusListener statusListener);

	boolean isEnabled();

	String getName();

	boolean isTradingLocked();

	/**
	 * @param secretKey
	 * @returns false if trading is unlocked
	 */
	boolean unlockTrading(String secretKey);

	void requestBalance(boolean overrideWorkingBalance) throws BalanceRequestException;

}
