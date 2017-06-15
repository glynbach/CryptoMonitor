package com.kieral.cryptomon.test.utlil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.kieral.cryptomon.model.accounting.TradingFeeType;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBookEntry;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;

public class TestUtils {

	private final static AtomicLong counter = new AtomicLong(0);
	
	public static CurrencyPair cp(Currency currency1, Currency currency2, BigDecimal fee) {
		return new CurrencyPair(currency1.name() + currency2.name(), currency1, currency2, 
				currency1.name() + "-" + currency2.name(), 8, fee,TradingFeeType.PERCENTAGE);
	}
	
	public static OrderBook ob(String market, CurrencyPair currency) {
		return ob(market, currency, counter.incrementAndGet());
	}

	public static OrderBook ob(String market, CurrencyPair currency, long sequenceNumber) {
		return new OrderBook(market, currency, sequenceNumber, System.currentTimeMillis());
	}

	public static OrderBook ob(String market, CurrencyPair currency,
			String[] bids, String[] asks) {
		return ob(market, currency, counter.incrementAndGet(), bids, asks);
	}

	public static OrderBook ob(String market, CurrencyPair currency, long sequenceNumber, 
			String[] bids, String[] asks) {
		String[] bidAmounts = bids == null ? null : new String[bids.length];
		if (bidAmounts != null) {
			for (int i=0; i<bidAmounts.length; i++)
				bidAmounts[i] = "1000";
		}
		String[] askAmounts = asks == null ? null : new String[asks.length];
		for (int i=0; i<bidAmounts.length; i++)
			askAmounts[i] = "1000";
		return ob(market, currency, bids, bidAmounts, asks, askAmounts);
	}

	public static OrderBook ob(String market, CurrencyPair currency,
			String[] bids, String bidAmounts[], String[] asks, String[] askAmounts) {
		return ob(market, currency, counter.incrementAndGet(), bids, bidAmounts, asks, askAmounts);
	}

	public static OrderBook ob(String market, CurrencyPair currency, long sequenceNumber,
			String[] bids, String bidAmounts[], String[] asks, String[] askAmounts) {
		OrderBook ob = new OrderBook(market, currency, sequenceNumber, System.currentTimeMillis());
		if (bids != null) {
			List<OrderBookEntry> bidEntries = new ArrayList<OrderBookEntry>();
			for (int i=0; i<bids.length; i++) {
				bidEntries.add(new ObEntry(Side.BID, new BigDecimal(bids[i]), new BigDecimal(bidAmounts[i])));
			}
			ob.setBids(bidEntries);
		}
		if (asks != null) {
			List<OrderBookEntry> askEntries = new ArrayList<OrderBookEntry>();
			for (int i=0; i<asks.length; i++) {
				askEntries.add(new ObEntry(Side.BID, new BigDecimal(asks[i]), new BigDecimal(askAmounts[i])));
			}
			ob.setAsks(askEntries);
		}
		return ob;
	}

	public static OrderBookConfig obConfig() {
		OrderBookConfig config = new OrderBookConfig();
		config.setDefaultSignificantAmount(new BigDecimal("0.01"));
		config.setCurrencies(Arrays.asList(new OrderBookConfig.CurrencySignificantAmount[] {
				csa(Currency.BTC, "0.01"),
				csa(Currency.LTC, "0.1"),
				csa(Currency.ETC, "0.1")
		}));
		config.setMarkets(Arrays.asList(new OrderBookConfig.MarketProperties[] {
				mp("bittrex", "0.05", new OrderBookConfig.CurrencySignificantAmount[] {
					csa(Currency.BTC, "0.05"),
					csa(Currency.LTC, "0.5"),
					csa(Currency.ETC, "0.5")
				})
		}));
		return config;
	}
	
	public static OrderBookConfig.CurrencySignificantAmount csa(Currency currency, String amount) {
		OrderBookConfig.CurrencySignificantAmount rtn = new OrderBookConfig.CurrencySignificantAmount();
		rtn.setCurrency(currency);
		rtn.setSignificantAmount(new BigDecimal(amount));
		return rtn;
	}
	
	public static OrderBookConfig.MarketProperties mp(String market, String defaultAmount, 
			OrderBookConfig.CurrencySignificantAmount... amounts) {
		OrderBookConfig.MarketProperties rtn = new OrderBookConfig.MarketProperties();
		rtn.setMarket(market);
		rtn.setDefaultSignificantAmount(new BigDecimal(defaultAmount));
		rtn.setCurrencies(amounts == null ? null : Arrays.asList(amounts));
		return rtn;
	}

	
	private static class ObEntry implements OrderBookEntry {

		private final Side side;
		private final BigDecimal price;
		private BigDecimal amount;
		
		private ObEntry(Side side, BigDecimal price, BigDecimal amount) {
			this.side = side;
			this.price = price;
			this.amount = amount;
		}
		
		@Override
		public Side getSide() {
			return side;
		}

		@Override
		public BigDecimal getPrice() {
			return price;
		}

		@Override
		public BigDecimal getAmount() {
			return amount;
		}

		@Override
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		@Override
		public String toString() {
			return "ObEntry [side=" + side + ", price=" + price + ", amount=" + amount + "]";
		}
		
	}
}
