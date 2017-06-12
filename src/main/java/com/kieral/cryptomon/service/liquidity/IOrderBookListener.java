package com.kieral.cryptomon.service.liquidity;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface IOrderBookListener {

	void onOrderBookUpdate(OrderBook orderBook);
	
}
