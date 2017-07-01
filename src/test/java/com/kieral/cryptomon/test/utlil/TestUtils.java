package com.kieral.cryptomon.test.utlil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBookEntry;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.service.exchange.bittrex.payload.BittrexOrderBookResponse;
import com.kieral.cryptomon.service.exchange.gdax.payload.GdaxOrderBookResponse;
import com.kieral.cryptomon.service.exchange.poloniex.payload.PoloniexOrderBookResponse;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;

public class TestUtils {

	private final static AtomicLong counter = new AtomicLong(0);

	public static List<CurrencyPair> createTestPairsList() {
		List<CurrencyPair> pairs = new ArrayList<CurrencyPair>();
		pairs.add(cp(Currency.ETH, Currency.BTC, new BigDecimal("0.25")));
		pairs.add(cp(Currency.LTC, Currency.BTC, new BigDecimal("0.25")));
		return pairs;
	}
	
	public static CurrencyPair cpFor(String name) {
		for (CurrencyPair pair : createTestPairsList()) {
			if (pair.getName().equals(name))
				return pair;
		}
		return null;
	}
	public static CurrencyPair cp(Currency currency1, Currency currency2, BigDecimal fee) {
		return new CurrencyPair(currency1.name() + currency2.name(), currency1, currency2, 
				currency1.name() + "-" + currency2.name(), 8, fee,TradingFeeType.PERCENTAGE);
	}
	
	public static OrderBook ob(String market, CurrencyPair currency) {
		return ob(market, currency, counter.incrementAndGet());
	}

	public static OrderBook ob(String market, CurrencyPair currency, long sequenceNumber) {
		return new OrderBook(market, currency, sequenceNumber, System.currentTimeMillis(), true);
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
		OrderBook ob = new OrderBook(market, currency, sequenceNumber, System.currentTimeMillis(), true);
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
		ob.setValid(true);
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

	public static String readResource(String resourceName) {
		BufferedReader reader = null;
		StringBuffer rtn = new StringBuffer();
		try {
			reader = new BufferedReader(new InputStreamReader(TestUtils.class.getResourceAsStream("abc.xml")));
			String line = null;
			while ((line = reader.readLine()) != null) {
				rtn.append(line + "\n");
			}
		} catch (Exception e) {
			Assert.fail("Cannot open resource " + resourceName);
		} finally {
			try {
				reader.close();
			} catch (Exception ex) {}
		}
		return rtn.toString();
	}

	public static void assertEquals(String expected, BigDecimal value) {
		if (expected == null && value == null)
			return;
		if (!isNumber(expected)) 
			Assert.fail("Expected " + expected + " but got " + value);
	}
	
	public static void assertEquals(BigDecimal expected, BigDecimal value) {
		if (expected == null) {
			if (value != null)
				Assert.fail("Expecred null but got " + value);
		} else {
			if (value == null)
				Assert.fail("Expecred " + expected + " but got null");
			else {
				if (expected.compareTo(value) != 0)
					Assert.fail("Expected " + expected + " but got " + value);
			}
		}
	}
	
	public static boolean isNumber(String value) {
		try {
			new BigDecimal(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static String getLogValue(String label, String seperator, String line) {
		if (line == null || label == null)
			return null;
		if (!label.endsWith("="))
			label = label.trim() + "=";
		int pos = line.indexOf(label);
		if (pos == -1)
			return null;
		int endPos = line.indexOf(seperator, pos);
		if (endPos == -1)
			endPos = line.length();
		return line.substring(pos + label.length(), endPos).trim();
	}

	public static List<List<String>> tokeniseLogArrayEntry(String logArrayEntry) {
		List<List<String>> rtn = new ArrayList<List<String>>();
		if (logArrayEntry == null || logArrayEntry.length() == 0)
			return rtn;
		String[] elements = logArrayEntry.replaceAll("\\[\\[", "[").replaceAll("\\]", "").split("\\[");
		for (String element : elements) {
			if (element.trim().length() > 0) {
				List<String> entries = new ArrayList<String>();
				for (String elementEntry : element.trim().split(","))
					entries.add(elementEntry.trim());
				rtn.add(entries);
			}
		}
		return rtn;
	}
	
	public static List<GdaxOrderBookResponse> convertGdaxLogs(CurrencyPair currencyPair, InputStream resourceStream) throws IOException {
		List<GdaxOrderBookResponse> rtn = new ArrayList<GdaxOrderBookResponse>();
		if (resourceStream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains("sequence")) {
						long sequence = Long.valueOf(getLogValue("sequence", ",", line));
						String bids = getLogValue("bids", "]]", line) + "]]";
						List<List<String>> bidEntries = tokeniseLogArrayEntry(bids);
						String asks = getLogValue("asks", "]]", line) + "]]";
						List<List<String>> askEntries = tokeniseLogArrayEntry(asks);
						GdaxOrderBookResponse response = new GdaxOrderBookResponse();
						response.setSequence(sequence);
						response.setCurrencyPair(currencyPair);
						response.setBids(bidEntries);
						response.setAsks(askEntries);
						rtn.add(response);
					}
				}
			} finally {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
		return rtn;
	}

	public static List<PoloniexOrderBookResponse> convertPoloniexLogs(CurrencyPair currencyPair, InputStream resourceStream) throws IOException {
		List<PoloniexOrderBookResponse> rtn = new ArrayList<PoloniexOrderBookResponse>();
		if (resourceStream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains("createdTime")) {
						long seq = Long.valueOf(getLogValue("seq", ",", line));
						String isFrozen = getLogValue("isFrozen", ",", line);
						String bids = getLogValue("bids", "]]", line) + "]]";
						List<List<String>> bidEntries = tokeniseLogArrayEntry(bids);
						String asks = getLogValue("asks", "]]", line) + "]]";
						List<List<String>> askEntries = tokeniseLogArrayEntry(asks);
						PoloniexOrderBookResponse response = new PoloniexOrderBookResponse();
						response.setSeq(seq);
						response.setIsFrozen(isFrozen);
						response.setCurrencyPair(currencyPair);
						response.setBids(bidEntries);
						response.setAsks(askEntries);
						rtn.add(response);
					}
				}
			} finally {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
		return rtn;
	}

	public static List<BittrexOrderBookResponse> convertBittrexLogs(CurrencyPair currencyPair, InputStream resourceStream) throws IOException {
		List<BittrexOrderBookResponse> rtn = new ArrayList<BittrexOrderBookResponse>();
		if (resourceStream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(resourceStream));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains("createdTime")) {
						boolean success  = Boolean.valueOf(getLogValue("success", ",", line));
						String message = getLogValue("message", ",", line);
						// parse result
						List<BittrexOrderBookResponse.Entry> buys = new ArrayList<BittrexOrderBookResponse.Entry>(); 
						List<BittrexOrderBookResponse.Entry> sells = new ArrayList<BittrexOrderBookResponse.Entry>(); 
						String buysFragment = line.substring(line.indexOf("buy="), line.indexOf("sell="));
						String sellsFragment = line.substring(line.indexOf("sell="));
						int pos = 0;
						while (pos >= 0) {
							BittrexOrderBookResponse.Entry entry = new BittrexOrderBookResponse.Entry();
							entry.setQuantity(new BigDecimal(getLogValue("quantity", ",", buysFragment)));
							String rate = getLogValue("rate", "],", buysFragment);
							// last rate in array will have ]]
							rate = rate.replaceAll("\\]", "");
							entry.setRate(new BigDecimal(rate));
							buys.add(entry);
							pos = buysFragment.indexOf("Entry", buysFragment.indexOf("Entry") + "Entry".length());
							if (pos >= 0)
								buysFragment = buysFragment.substring(pos);
						}
						pos = 0;
						while (pos >= 0) {
							BittrexOrderBookResponse.Entry entry = new BittrexOrderBookResponse.Entry();
							entry.setQuantity(new BigDecimal(getLogValue("quantity", ",", sellsFragment)));
							String rate = getLogValue("rate", "],", sellsFragment);
							// last rate in array will have ]]
							rate = rate.replaceAll("\\]", "");
							entry.setRate(new BigDecimal(rate));
							sells.add(entry);
							pos = sellsFragment.indexOf("Entry", sellsFragment.indexOf("Entry") + "Entry".length());
							if (pos >= 0)
								sellsFragment = sellsFragment.substring(pos);
						}
						BittrexOrderBookResponse.Result result = new BittrexOrderBookResponse.Result();
						result.setBuy(buys);
						result.setSell(sells);
						BittrexOrderBookResponse response = new BittrexOrderBookResponse();
						response.setSuccess(success);
						response.setMessage(message);
						response.setCurrencyPair(currencyPair);
						response.setResult(result);
						rtn.add(response);
					}
				}
			} finally {
				try {
					reader.close();
				} catch (Exception e) {}
			}
		}
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
