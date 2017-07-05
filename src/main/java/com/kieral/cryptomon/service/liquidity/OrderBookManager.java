package com.kieral.cryptomon.service.liquidity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.LiquidityEntry;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBookEntry;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.orderbook.OrderBookAction;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;

public class OrderBookManager {

	public static final long AUTO_INCREMENT_SEQUENCE = -1L;
	
	private static final PriceComparer BID_COMPARER = new PriceComparer(Side.BID);  
	private static final PriceComparer ASK_COMPARER = new PriceComparer(Side.ASK);  

	private final ConcurrentMap<OrderBookKey, OrderBook> orderBooks = new ConcurrentHashMap<OrderBookKey, OrderBook>(); 
	private final ConcurrentMap<OrderBookKey, AtomicInteger> orderBookSequencers = new ConcurrentHashMap<OrderBookKey, AtomicInteger>(); 
	private final ConcurrentMap<OrderBookKey, Object> orderBookLocks = new ConcurrentHashMap<OrderBookKey, Object>(); 

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	OrderBookConfig orderBookConfig;
	@Autowired
	OrderBookSanityChecker orderBookSanityChecker;
	
	@PostConstruct
	public void init() {
		logger.info("OrderbookManager using config {} and sanity checker {}", orderBookConfig, orderBookSanityChecker);
	}

	public OrderBook getOrderBook(OrderBookResponse orderBookResponse, String market, 
			CurrencyPair currencyPair, int maxLevel) {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currencyPair == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		clearOrderBook(market, currencyPair);
		if (orderBookResponse == null) {
			return updateOrderBook(market, currencyPair, null, 0, System.currentTimeMillis(), false, 0);
		}
		List<OrderBookUpdate> updates = new ArrayList<OrderBookUpdate>();
		if(orderBookResponse.getBidEntries() != null) {
			updates.addAll(getUpdatesFromEntries(Side.BID, orderBookResponse.getBidEntries()));
			updates.addAll(getUpdatesFromEntries(Side.ASK, orderBookResponse.getAskEntries()));
		}
		return updateOrderBook(market, currencyPair, updates, orderBookResponse.getSequence(),
				orderBookResponse.getCreatedTime(), orderBookResponse.isValid(), maxLevel);
	}
	
	private List<OrderBookUpdate> getUpdatesFromEntries(Side side, List<OrderBookResponseEntry> entries) {
		List<OrderBookUpdate> updates = new ArrayList<OrderBookUpdate>();
		if (entries != null) {
			entries.forEach(entry -> {
				updates.add(new OrderBookUpdate(side, entry.getPrice(), entry.getAmount(), OrderBookAction.REPLACE));
			});
		}
		return updates;
	}

	public void clearOrderBook(String market, CurrencyPair currencyPair) {
		OrderBookKey key = new OrderBookKey(market, currencyPair.getName());
		orderBooks.remove(key);
	}

	/**
	 * Sending a sequence number of -1 will auto-increment the orderbook sequence 
	 */
	public OrderBook updateOrderBook(String market, CurrencyPair currencyPair, List<OrderBookUpdate> updates, 
			long sequenceNumber, long updatesReceiedTime, boolean valid, int maxLevel) {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currencyPair == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (logger.isDebugEnabled())
			logger.debug("Received updates for {} from {} with seq {} and maxLevel {} - {}", currencyPair.getName(),
					market, sequenceNumber, maxLevel, updates);
		OrderBookKey key = new OrderBookKey(market, currencyPair.getName());
		orderBooks.putIfAbsent(key, new OrderBook(market, currencyPair, 
				sequenceNumber == AUTO_INCREMENT_SEQUENCE ? 0 : sequenceNumber, updatesReceiedTime <= 0 ? System.currentTimeMillis() : updatesReceiedTime, valid));
		if (sequenceNumber == AUTO_INCREMENT_SEQUENCE)
			orderBookSequencers.putIfAbsent(key, new AtomicInteger(0));
		OrderBook orderBook = orderBooks.get(key);
		orderBookLocks.putIfAbsent(key, new Object());
		Object lock = orderBookLocks.get(key);
		synchronized(lock) {
			List <OrderBookEntry> bids;
			List <OrderBookEntry> asks;
			if (updates == null || updates.size() == 0) {
				bids = Collections.emptyList();
				asks = Collections.emptyList();
			} else {
				final AtomicBoolean actioned = new AtomicBoolean(false);
				final AtomicBoolean hasBid = new AtomicBoolean(false);
				final AtomicBoolean hasAsk = new AtomicBoolean(false);
				bids = new ArrayList<OrderBookEntry>(orderBook.getBids());
				asks = new ArrayList<OrderBookEntry>(orderBook.getAsks());
				updates.forEach(update -> {
					OrderBookEntry entry = update.getEntry();
					hasBid.compareAndSet(false, entry.getSide() == Side.BID);
					hasAsk.compareAndSet(false, entry.getSide() == Side.ASK);
					OrderBookAction action = update.getAction();
					List<OrderBookEntry> entries = entry.getSide() == Side.BID ? bids : asks;
					Iterator<OrderBookEntry> i = entries.iterator();
					while (i.hasNext()) {
						OrderBookEntry bookEntry = i.next();
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
					}
				});
				if (logger.isDebugEnabled())
					logger.debug("Ended update detected hasBid={} hasAsk={}", hasBid.get(), hasAsk.get());
				if (hasBid.get()) {
					if (logger.isDebugEnabled())
						logger.debug("Sorting bids {}", bids);
					bids.sort(BID_COMPARER);
					if (bids.size() > maxLevel) {
						if (logger.isDebugEnabled())
							logger.debug("Truncating bids {}", bids);
						bids.subList(maxLevel, bids.size()).clear();
					}
					if (logger.isDebugEnabled())
						logger.debug("Setting bids {}", bids);
					orderBook.setBids(bids);
				}
				if (hasAsk.get()) {
					if (logger.isDebugEnabled())
						logger.debug("Sorting asks {}", asks);
					asks.sort(ASK_COMPARER);
					if (asks.size() > maxLevel) {
						if (logger.isDebugEnabled())
							logger.debug("Truncating asks {}", asks);
						asks.subList(maxLevel, asks.size()).clear();
					}
					if (logger.isDebugEnabled())
						logger.debug("Setting asks {}", asks);
					orderBook.setAsks(asks);
				}
			}
			orderBook.setSnapshotReceived(updatesReceiedTime <= 0 ? System.currentTimeMillis() : updatesReceiedTime);
			if (sequenceNumber == AUTO_INCREMENT_SEQUENCE)
				sequenceNumber = orderBookSequencers.getOrDefault(key, new AtomicInteger()).incrementAndGet();
			orderBook.setSnapshotSequence(sequenceNumber);
			if (valid && orderBookSanityChecker != null)
				valid = orderBookSanityChecker.isValid(orderBook);
			orderBook.setValid(valid);
		}
		return orderBook;
	}

	public boolean isValidPrice(Side side, CurrencyPair pair, BigDecimal price) {
		if (orderBookSanityChecker != null) {
			return orderBookSanityChecker.isValid(side, pair, price);
		} else {
			logger.warn("Cannot sanity check price as orderBookSanityChecker is null");
			return true;
		}
	}

	public LiquidityEntry getBestBidAsk(String market, CurrencyPair currencyPair, BigDecimal amount) {
		if (market == null || currencyPair == null)
			return null;
		return OrderBookManager.getBestBidAsk(orderBooks.get(new OrderBookKey(market, currencyPair.getName())), amount);
	}

	public LiquidityEntry getBestBidAsk(OrderBook orderBook) {
		return OrderBookManager.getBestBidAsk(orderBook, orderBookConfig.getSignificantAmount(orderBook.getMarket(), orderBook.getCurrencyPair().getBaseCurrency()));
	}

	public static LiquidityEntry getBestBidAsk(OrderBook orderBook, BigDecimal desiredAmount) {
		if (orderBook == null)
			return null;
		List<OrderBookEntry> bidEntries = getDepthNeeded(Side.BID, orderBook.getMarket(), 
				orderBook.getCurrencyPair().getBaseCurrency(), orderBook.getBids(), desiredAmount);
		BigDecimal bidAmount = bidEntries == null ? BigDecimal.ZERO :
			bidEntries.stream().map(OrderBookEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal bidPrice = bidEntries == null ? null : bidEntries.get(bidEntries.size() - 1).getPrice();
		List<OrderBookEntry> askEntries = getDepthNeeded(Side.ASK, orderBook.getMarket(), 
				orderBook.getCurrencyPair().getBaseCurrency(), orderBook.getAsks(), desiredAmount);
		BigDecimal askAmount = askEntries == null ? BigDecimal.ZERO :
			askEntries.stream().map(OrderBookEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal askPrice = askEntries == null ? null : askEntries.get(askEntries.size() - 1).getPrice();
		return new LiquidityEntry(new BidAskPrice(bidPrice, askPrice), new BidAskAmount(new TradeAmount(bidAmount, askPrice, orderBook.getCurrencyPair().getPriceScale())
				, new TradeAmount(askAmount, bidPrice, orderBook.getCurrencyPair().getPriceScale())));
	}

	private static List<OrderBookEntry> getDepthNeeded(Side side, String market, Currency baseCurrency, List<OrderBookEntry> entries, BigDecimal desiredAmount) {
		List<OrderBookEntry> rtn = new ArrayList<OrderBookEntry>();
		BigDecimal cumulativeAmount = BigDecimal.ZERO;
		for (OrderBookEntry entry : entries) {
			rtn.add(entry);
			cumulativeAmount = cumulativeAmount.add(entry.getAmount());
			if (desiredAmount == null) {
				return rtn;
			} else if (desiredAmount.compareTo(cumulativeAmount) < 0) {
				return rtn;
			}
		}
		return null;
	}
	
	private static final class PriceComparer implements Comparator<OrderBookEntry> {

		private final Side side;
		
		private PriceComparer(Side side) {
			if (side == null)
				throw new IllegalArgumentException("side can not be nuil");
			this.side = side;
		}
	
		@Override
		public int compare(OrderBookEntry o1, OrderBookEntry o2) {
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
