package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface ArbService {

	void suspend(boolean suspend);
	boolean isSuspended();
	OrderBook getOrderBook(String market, String currencyPairName);
	ArbInstruction calculateArb(OrderBook market1, OrderBook market2, BigDecimal amountMarket1, BigDecimal amountMarket2);

}
