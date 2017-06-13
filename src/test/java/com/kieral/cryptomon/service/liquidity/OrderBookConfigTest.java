package com.kieral.cryptomon.service.liquidity;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class OrderBookConfigTest {

	private OrderBookConfig config;
	
	@Before
	public void setUp() {
		config = new OrderBookConfig();
		config.setDefaultSignificantAmount(new BigDecimal("0.01"));
		config.setCurrencies(Arrays.asList(new OrderBookConfig.CurrencySignificantAmount[] {
				TestUtils.csa(Currency.BTC, "0.01"),
				TestUtils.csa(Currency.LTC, "0.1"),
				TestUtils.csa(Currency.ETC, "0.1")
		}));
		config.setMarkets(Arrays.asList(new OrderBookConfig.MarketProperties[] {
				TestUtils.mp("bittrex", "0.05", new OrderBookConfig.CurrencySignificantAmount[] {
						TestUtils.csa(Currency.BTC, "0.05"),
						TestUtils.csa(Currency.LTC, "0.5"),
						TestUtils.csa(Currency.ETC, "0.5")
				})
		}));
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
