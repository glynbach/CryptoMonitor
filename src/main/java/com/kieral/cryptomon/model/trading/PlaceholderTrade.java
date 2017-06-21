package com.kieral.cryptomon.model.trading;

import java.math.BigDecimal;

public class PlaceholderTrade extends Trade {

	public PlaceholderTrade(BigDecimal amount) {
		super(null, null, amount, BigDecimal.ZERO, true, 0);
	}

}
