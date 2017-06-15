package com.kieral.cryptomon.tickstore;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface OrderBookDao {

	void saveOrderBookDataEntries(OrderBook orderBook);
	
}
