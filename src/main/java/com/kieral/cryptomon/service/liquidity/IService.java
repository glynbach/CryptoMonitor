package com.kieral.cryptomon.service.liquidity;

import com.kieral.cryptomon.service.connection.IStatusListener;

public interface IService {

	void connect() throws InterruptedException;
	
	void disconnect();
	
	void registerOrderBookListener(IOrderBookListener orderBookListener);
	
	void registerStatusListener(IStatusListener statusListener);
	
}
