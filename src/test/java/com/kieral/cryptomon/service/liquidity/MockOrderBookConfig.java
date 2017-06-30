package com.kieral.cryptomon.service.liquidity;

import java.math.BigDecimal;
import java.util.Arrays;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class MockOrderBookConfig extends OrderBookConfig {
	
	public MockOrderBookConfig() {
		setDefaultSignificantAmount(new BigDecimal("0.01"));
		setCurrencies(Arrays.asList(new OrderBookConfig.CurrencySignificantAmount[] {
				TestUtils.csa(Currency.BTC, "0.01"),
				TestUtils.csa(Currency.LTC, "0.1"),
				TestUtils.csa(Currency.ETC, "0.1")
		}));
		setMarkets(Arrays.asList(new OrderBookConfig.MarketProperties[] {
				TestUtils.mp("bittrex", "0.05", new OrderBookConfig.CurrencySignificantAmount[] {
						TestUtils.csa(Currency.BTC, "0.05"),
						TestUtils.csa(Currency.LTC, "0.5"),
						TestUtils.csa(Currency.ETC, "0.5")
				})
		}));
		init();
	}

}
