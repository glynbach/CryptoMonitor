package com.kieral.cryptomon.service.arb;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface ArbExaminer {

	ArbInstruction examine(OrderBook market1, OrderBook market2);
	
}
