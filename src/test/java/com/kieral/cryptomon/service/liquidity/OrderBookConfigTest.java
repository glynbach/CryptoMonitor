package com.kieral.cryptomon.service.liquidity;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import com.kieral.cryptomon.model.general.Currency;

public class OrderBookConfigTest {

	private MockOrderBookConfig config;
	
	@Before
	public void setUp() {
		config = new MockOrderBookConfig();
	}
	
	@Test
	public void testMarketCurrencyUsed() {
		assertFalse(config.isSignificant("bittrex", Currency.LTC, new BigDecimal("0.4")));
		assertTrue(config.isSignificant("bittrex", Currency.LTC, new BigDecimal("0.5")));
	}

	@Test
	public void testMarketDefaultUsed() {
		assertFalse(config.isSignificant("bittrex", Currency.DASH, new BigDecimal("0.04")));
		assertTrue(config.isSignificant("bittrex", Currency.DASH, new BigDecimal("0.05")));
	}

	@Test
	public void testCurrencyDefaultUsed() {
		assertFalse(config.isSignificant("poloniex", Currency.LTC, new BigDecimal("0.09")));
		assertTrue(config.isSignificant("poloniex", Currency.LTC, new BigDecimal("0.1")));
	}

	@Test
	public void testDefaultUsed() {
		assertFalse(config.isSignificant("poloniex", Currency.DASH, new BigDecimal("0.009")));
		assertTrue(config.isSignificant("poloniex", Currency.DASH, new BigDecimal("0.01")));
	}

}
