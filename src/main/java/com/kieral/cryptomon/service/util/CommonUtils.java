package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CommonUtils {

	public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0000");
	
	public static BigDecimal getTradingfeeMultiplier(BigDecimal fee) {
		return ONE_HUNDRED.subtract(fee).divide(ONE_HUNDRED, RoundingMode.HALF_UP);
	}

}
