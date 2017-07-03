package com.kieral.cryptomon.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class CommonUtils {

	public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00000000");
	public static final DateTimeFormatter SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss");
	public static final DateTimeFormatter MILLIS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss.SSS");
	
	public static BigDecimal getTradingfeeMultiplier(BigDecimal fee) {
		return ONE_HUNDRED.subtract(fee).divide(ONE_HUNDRED, RoundingMode.HALF_UP);
	}

	public static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	public static long getMillis(String value, DateTimeFormatter dtf, long defaultValue) {
		try {
			return LocalDateTime.parse(value, dtf).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public static BigDecimal returnLargestOf(BigDecimal first, BigDecimal second) {
		if (first == null) {
			if (second == null)
				return BigDecimal.ZERO;
			return second;
		}
		if (second == null)
			return first;
		if (first.compareTo(second) > 0)
			return first;
		return second;
	}
	
	public static boolean isNumber(String string) {
		try {
			new BigDecimal(string);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isZero(BigDecimal value) {
		return BigDecimal.ZERO.compareTo(value) == 0;
	}

	public static boolean isAtLeast(BigDecimal value, BigDecimal minimum) {
		if (minimum == null)
			return true;
		if (value == null)
			return false;
		return value.compareTo(minimum) >= 0;
	}

	public static boolean isAtLeast(String value, String minimum) {
		if (!isNumber(value))
			return false;
		if (!isNumber(minimum))
			return true;
		return isAtLeast(new BigDecimal(value), new BigDecimal(minimum));
	}

	public static BigDecimal percentageChange(BigDecimal from, BigDecimal to) {
		if (from == null || to == null)
			return ONE_HUNDRED;
		return to.subtract(from).abs().divide(from, 8, RoundingMode.HALF_UP).multiply(ONE_HUNDRED);
	}

	public static BigDecimal getPercentageOf(BigDecimal amount, BigDecimal percentage) {
		return amount.multiply(percentage).divide(ONE_HUNDRED, 8, RoundingMode.HALF_DOWN);
	}

	public static void main(String[] args) {
		System.out.println(getPercentageOf(ONE_HUNDRED, new BigDecimal("99")));
		System.out.println(getPercentageOf(new BigDecimal("8.1388"), new BigDecimal("99")));
	}
}
