package com.kieral.cryptomon.service.exchange;

import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.liquidity.IOrderBookListener;

public interface IExchangeService {

	void connect() throws InterruptedException;
	
	void disconnect();
	
	void registerOrderBookListener(IOrderBookListener orderBookListener);
	
	void registerStatusListener(IStatusListener statusListener);

	boolean isEnabled();

	String getName();
	
}
