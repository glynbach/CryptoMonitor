package com.kieral.cryptomon.model;

import java.math.BigDecimal;

public interface IOrderBookEntry {

	Side getSide();
	BigDecimal getPrice();
	BigDecimal getAmount();
	void setAmount(BigDecimal amount);
	
}
