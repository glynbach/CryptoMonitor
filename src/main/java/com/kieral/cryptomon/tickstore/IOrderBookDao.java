package com.kieral.cryptomon.tickstore;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface IOrderBookDao {

	void saveOrderBookDataEntries(OrderBook orderBook);
	
}
