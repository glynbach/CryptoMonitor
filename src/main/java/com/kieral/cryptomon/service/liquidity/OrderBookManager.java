package com.kieral.cryptomon.service.liquidity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.IOrderBookEntry;
import com.kieral.cryptomon.model.OrderBookAction;
import com.kieral.cryptomon.model.OrderBookUpdate;
import com.kieral.cryptomon.model.Side;

public class OrderBookManager {

	private static final PriceComparer BID_COMPARER = new PriceComparer(Side.BID);  
	private static final PriceComparer ASK_COMPARER = new PriceComparer(Side.ASK);  

	private final ConcurrentMap<OrderBookKey, OrderBook> orderBooks = new ConcurrentHashMap<OrderBookKey, OrderBook>(); 
	private final ConcurrentMap<OrderBookKey, Object> orderBookLocks = new ConcurrentHashMap<OrderBookKey, Object>(); 

	public OrderBook clearOrderBook(String market, CurrencyPair currencyPair) {
		return updateOrderBook(market, currencyPair, null, true);
	}

	public OrderBook updateOrderBook(String market, CurrencyPair currencyPair, List<OrderBookUpdate> updates) {
		return updateOrderBook(market, currencyPair, updates, false);
	}
	
	public OrderBook updateOrderBook(String market, CurrencyPair currencyPair, List<OrderBookUpdate> updates, boolean clear) {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currencyPair == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		OrderBookKey key = new OrderBookKey(market, currencyPair.getName());
		orderBooks.putIfAbsent(key, new OrderBook(market, currencyPair));
		OrderBook orderBook = orderBooks.get(key);
		orderBookLocks.putIfAbsent(key, new Object());
		Object lock = orderBookLocks.get(key);
		synchronized(lock) {
			if (clear) {
				orderBook.setAsks(Collections.emptyList());
				orderBook.setBids(Collections.emptyList());
			}
			if (updates != null) {
				final AtomicBoolean actioned = new AtomicBoolean(false);
				for (OrderBookUpdate update : updates) {
					actioned.set(false);
					IOrderBookEntry entry = update.getEntry();
					OrderBookAction action = update.getAction();
					List<IOrderBookEntry> entries = new ArrayList<IOrderBookEntry>(entry.getSide() == Side.BID ? orderBook.getBids() : orderBook.getAsks());
					PriceComparer comparer = entry.getSide() == Side.BID ? BID_COMPARER : ASK_COMPARER;
					entries.sort(comparer);
					Iterator<IOrderBookEntry> i = entries.iterator();
					while (i.hasNext()) {
						IOrderBookEntry bookEntry = i.next();
						if (bookEntry.getPrice().compareTo(entry.getPrice()) == 0) {
							if (action == OrderBookAction.REMOVE)
								i.remove();
							if (action == OrderBookAction.REPLACE) {
								bookEntry.setAmount(entry.getAmount());
								actioned.set(true);
							}
						}
					}
					if (action == OrderBookAction.REPLACE && !actioned.get()) {
						entries.add(entry);
						entries.sort(comparer);
					}
					if (entry.getSide() == Side.BID)
						orderBook.setBids(entries); 
					else
						orderBook.setAsks(entries);
				}
			}
		}
		return orderBook;
	}
	
	private static final class PriceComparer implements Comparator<IOrderBookEntry> {

		private final Side side;
		
		private PriceComparer(Side side) {
			if (side == null)
				throw new IllegalArgumentException("side can not be nuil");
			this.side = side;
		}
	
		@Override
		public int compare(IOrderBookEntry o1, IOrderBookEntry o2) {
			// let it NPE if a null price has got through
			int result = o1.getPrice().compareTo(o2.getPrice());
			if (side == Side.BID)
				result = result * -1;
			return result;
		}
		
	}
	
	private final class OrderBookKey {
		private final String market;
		private final String currencyPair;
		public OrderBookKey(String market, String currencyPair) {
			super();
			this.market = market;
			this.currencyPair = currencyPair;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((currencyPair == null) ? 0 : currencyPair.hashCode());
			result = prime * result + ((market == null) ? 0 : market.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OrderBookKey other = (OrderBookKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (currencyPair == null) {
				if (other.currencyPair != null)
					return false;
			} else if (!currencyPair.equals(other.currencyPair))
				return false;
			if (market == null) {
				if (other.market != null)
					return false;
			} else if (!market.equals(other.market))
				return false;
			return true;
		}
		private OrderBookManager getOuterType() {
			return OrderBookManager.this;
		}
		@Override
		public String toString() {
			return "OrderBookKey [market=" + market + ", currencyPair=" + currencyPair + "]";
		}
	}
}
