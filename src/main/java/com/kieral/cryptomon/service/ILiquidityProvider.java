package com.kieral.cryptomon.service;

public interface ILiquidityProvider {

	void connect() throws InterruptedException;
	
	void disconnect();
	
	void registerOrderBookListener(IOrderBookListener orderBookListener);
	
	void registerStatusListener(IStatusListener statusListener);
	
}
