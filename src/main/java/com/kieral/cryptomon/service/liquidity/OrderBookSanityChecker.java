package com.kieral.cryptomon.service.liquidity;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;

public interface OrderBookSanityChecker {

	boolean isValid(OrderBook orderBook);
	boolean isValid(Side side, CurrencyPair pair, BigDecimal price);
	
}
