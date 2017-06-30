package com.kieral.cryptomon.service.liquidity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.Currency;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="orderbook")
public class OrderBookConfig {
	
	private final static BigDecimal DEFAULT_SIGNIFICANT_AMOUNT = new BigDecimal("0.001");
	
	private BigDecimal defaultSignificantAmount;
	private List<CurrencySignificantAmount> currencies;
	private List<MarketProperties> markets;

	private final Map<String, BigDecimal> marketDefaultSignificantAmounts = new HashMap<String, BigDecimal>();
	private final Map<String, Map<Currency, BigDecimal>> marketSignificantAmounts = new HashMap<String, Map<Currency, BigDecimal>>();
	private final Map<Currency, BigDecimal> significantAmounts = new HashMap<Currency, BigDecimal>();

	public BigDecimal getDefaultSignificantAmount() {
		return defaultSignificantAmount;
	}

	public void setDefaultSignificantAmount(BigDecimal defaultSignificantAmount) {
		this.defaultSignificantAmount = defaultSignificantAmount;
	}

	public List<CurrencySignificantAmount> getCurrencies() {
		return currencies;
	}

	public void setCurrencies(List<CurrencySignificantAmount> currencies) {
		this.currencies = currencies;
	}

	public List<MarketProperties> getMarkets() {
		return markets;
	}

	public void setMarkets(List<MarketProperties> markets) {
		this.markets = markets;
	}

	@PostConstruct
	public void init() {
		if (defaultSignificantAmount == null)
			defaultSignificantAmount = DEFAULT_SIGNIFICANT_AMOUNT;
		if (currencies != null) {
			currencies.forEach(csa -> {
				significantAmounts.put(csa.getCurrency(), csa.getSignificantAmount());
			});
		}
		if (markets != null) {
			markets.forEach(mp -> {
				if (mp.getDefaultSignificantAmount() != null)
					marketDefaultSignificantAmounts.put(mp.getMarket(), mp.getDefaultSignificantAmount());
				if (mp.getCurrencies() != null) {
					mp.getCurrencies().forEach(csa -> {
						if (!marketSignificantAmounts.containsKey(mp.getMarket()))
							marketSignificantAmounts.put(mp.getMarket(), new HashMap<Currency, BigDecimal>());
						marketSignificantAmounts.get(mp.getMarket()).put(csa.getCurrency(), csa.getSignificantAmount());
					});
				}
			});
		}
	}

	public boolean isSignificant(String market, Currency currency, BigDecimal amount) {
		if (amount == null)
			return false;
		if (marketSignificantAmounts.containsKey(market)) {
			if (marketSignificantAmounts.get(market).containsKey(currency)) {
				if (amount.compareTo(marketSignificantAmounts.get(market).get(currency)) >= 0)
					return true;
				else
					return false;
			}
		}
		if (marketDefaultSignificantAmounts.containsKey(market)) {
			if (amount.compareTo(marketDefaultSignificantAmounts.get(market)) >= 0)
				return true;
			else
				return false;
		}
		if (significantAmounts.containsKey(currency)) {
			if (amount.compareTo(significantAmounts.get(currency)) >= 0)
				return true;
			else
				return false;
		}
		if (amount.compareTo(defaultSignificantAmount) >= 0)
			return true;
		return false;
	}
	
	public static class MarketProperties {
		
		private String market;
		private BigDecimal defaultSignificantAmount;
		private List<CurrencySignificantAmount> currencies;
		
		public String getMarket() {
			return market;
		}
		public void setMarket(String market) {
			this.market = market;
		}
		public BigDecimal getDefaultSignificantAmount() {
			return defaultSignificantAmount;
		}
		public void setDefaultSignificantAmount(BigDecimal defaultSignificantAmount) {
			this.defaultSignificantAmount = defaultSignificantAmount;
		}
		public List<CurrencySignificantAmount> getCurrencies() {
			return currencies;
		}
		public void setCurrencies(List<CurrencySignificantAmount> currencies) {
			this.currencies = currencies;
		}
		@Override
		public String toString() {
			return "MarketProperties [market=" + market + ", defaultSignificantAmount=" + defaultSignificantAmount
					+ ", currencies=" + currencies + "]";
		}
		
		
	}
	
	public static class CurrencySignificantAmount {
		
		private Currency currency;
		private BigDecimal significantAmount;
		
		public Currency getCurrency() {
			return currency;
		}
		public void setCurrency(Currency currency) {
			this.currency = currency;
		}
		public BigDecimal getSignificantAmount() {
			return significantAmount;
		}
		public void setSignificantAmount(BigDecimal significantAmount) {
			this.significantAmount = significantAmount;
		}
		@Override
		public String toString() {
			return "CurrencySignificantAmount [currency=" + currency + ", significantAmount=" + significantAmount + "]";
		}
		
	}

	@Override
	public String toString() {
		return "OrderBookConfig [defaultSignificantAmount=" + defaultSignificantAmount + ", currencies=" + currencies
				+ ", markets=" + markets + "]";
	}
	
}
