package com.kieral.cryptomon.service.liquidity;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface OrderBookListener {

	void onOrderBookUpdate(OrderBook orderBook);
	
}
