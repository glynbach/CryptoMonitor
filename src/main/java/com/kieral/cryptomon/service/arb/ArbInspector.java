package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface ArbInspector {

	ArbInstruction examine(OrderBook market1, OrderBook market2);
	ArbInstruction resolve(OrderBook longBook, OrderBook shortBook, BigDecimal longAmountRemaining, BigDecimal shortAmountRemaining);
	
}
