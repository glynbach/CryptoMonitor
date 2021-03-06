package com.kieral.cryptomon.model.orderbook;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;

public interface OrderBookEntry {

	Side getSide();
	BigDecimal getPrice();
	BigDecimal getAmount();
	void setAmount(BigDecimal amount);
	
}
