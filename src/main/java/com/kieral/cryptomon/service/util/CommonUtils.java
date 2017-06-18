package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class CommonUtils {

	public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0000");
	public static final DateTimeFormatter SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss");
	public static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm:ss.SSS");
	
	public static BigDecimal getTradingfeeMultiplier(BigDecimal fee) {
		return ONE_HUNDRED.subtract(fee).divide(ONE_HUNDRED, RoundingMode.HALF_UP);
	}

	public static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

}
