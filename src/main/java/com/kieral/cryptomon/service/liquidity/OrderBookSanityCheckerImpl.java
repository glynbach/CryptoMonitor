package com.kieral.cryptomon.service.liquidity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.orderbook.OrderBookEntry;
import com.kieral.cryptomon.service.util.CommonUtils;

public class OrderBookSanityCheckerImpl implements OrderBookSanityChecker{

	private final static int MIN_PERMITTED_LEVELS = 10;
	private final static BigDecimal MIN_PERMITTED_PERCENTAGE_PRICE_CHANGE = new BigDecimal("3.00000000");

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final static ConcurrentMap<String, BigDecimal> lastBestBid = new ConcurrentHashMap<String, BigDecimal>();
	private final static ConcurrentMap<String, BigDecimal> lastBestAsk = new ConcurrentHashMap<String, BigDecimal>();
	
	/**
	 * Attempt to check
	 *  - ordered in the right direction
	 *  - is the right book
	 *  - deep enough to be meaningful
	 *  - not crossed
	 */
	@Override
	public boolean isValid(OrderBook orderBook) {
		if (orderBook == null)
			return false;
		if (orderBook.getBids() == null || orderBook.getBids().size() < MIN_PERMITTED_LEVELS
				|| orderBook.getAsks() == null || orderBook.getAsks().size() < MIN_PERMITTED_LEVELS)
			return false;
		if (!sanityCheckLevels(Side.BID, orderBook.getBids()))
			return false;
		if (!sanityCheckLevels(Side.ASK, orderBook.getAsks()))
			return false;
		BigDecimal bestBid = orderBook.getBids().get(0).getPrice();
		BigDecimal bestAsk = orderBook.getAsks().get(0).getPrice();
		if (!isValid(Side.BID, orderBook.getCurrencyPair(), bestBid)) {
			logger.warn("Excessive change from previous best bid {} to new best bid {} for orderbook {}", 
					lastBestBid.get(orderBook.getCurrencyPair().getName()), bestBid, orderBook);
			return false;
		}
		if (!isValid(Side.ASK, orderBook.getCurrencyPair(), bestAsk)) {
			logger.warn("Excessive change from previous best ask {} to new best ask {} for orderbook {}", 
					lastBestAsk.get(orderBook.getCurrencyPair().getName()), bestAsk, orderBook);
			return false;
		}
		lastBestBid.put(orderBook.getCurrencyPair().getName(), bestBid);
		lastBestAsk.put(orderBook.getCurrencyPair().getName(), bestAsk);
		return true;
	}

	private boolean sanityCheckLevels(Side side, List<OrderBookEntry> entries) {
		BigDecimal largestTopOfBookJump = BigDecimal.ZERO;
		List<BigDecimal> totalJumps = new ArrayList<BigDecimal>();
		BigDecimal lastPrice = null;
		int count = 0;
		for (OrderBookEntry entry: entries) {
			if (lastPrice != null) {
				if (side == Side.BID && lastPrice.compareTo(entry.getPrice()) <= 0) {
					logger.warn("last bid price {} greater than next level price {} for entries {}", lastPrice.toPlainString(), 
							entry.getPrice().toPlainString(), entries);
					return false;
				}
				if (side == Side.ASK && lastPrice.compareTo(entry.getPrice()) >= 0) {
					logger.warn("last ask price {} less than next level price {} for entries {}", lastPrice.toPlainString(), 
							entry.getPrice().toPlainString(), entries);
					return false;
				}
				BigDecimal jump = entry.getPrice().subtract(lastPrice).abs();
				if (count < 3) {
					if (jump.compareTo(largestTopOfBookJump) > 0)
						largestTopOfBookJump = jump;
				}
				totalJumps.add(jump);
			}
			count++;
			lastPrice = entry.getPrice();
		}
		totalJumps.sort((BigDecimal o1, BigDecimal o2) -> o1.compareTo(o2));
		BigDecimal averageJump = totalJumps.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
				.divide(new BigDecimal(totalJumps.size()), 8, RoundingMode.HALF_UP);
		BigDecimal medianJump = totalJumps.get(totalJumps.size() / 2);
		if (largestTopOfBookJump.compareTo(averageJump.multiply(new BigDecimal("5.00000000"))) > 0) {
			logger.warn("largest jump at the top of the book {} was more than 5 times "
					+ "higher than average jump {} for entries {}",
					largestTopOfBookJump.toPlainString(), averageJump.toPlainString(), entries);
			// taking no action beyond logging a warning as this is a common occurrence
		}
		if (largestTopOfBookJump.compareTo(medianJump.multiply(new BigDecimal("10.00000000"))) > 0) {
			logger.warn("largest jump at the top of the book {} was more than 10 times "
					+ "higher than median jump {} for entries {}",
					largestTopOfBookJump.toPlainString(), medianJump.toPlainString(), entries);
			// taking no action beyond logging a warning as this is a common occurrence
		}
		return true;
	}

	@Override
	public boolean isValid(Side side, CurrencyPair pair, BigDecimal price) {
		if (side == null || pair == null || price == null)
			return false;
		ConcurrentMap<String, BigDecimal> last = side == Side.BID ? lastBestBid : lastBestAsk;
		if (last.containsKey(pair.getName())) {
			BigDecimal prevBest = last.get(pair.getName());
			BigDecimal percChange = CommonUtils.percentageChange(prevBest, price);
			if (percChange.compareTo(MIN_PERMITTED_PERCENTAGE_PRICE_CHANGE) > 0) {
				logger.warn("{} price {} for {} is a {} percentage change from last best of {}",
						side.name(), price.toPlainString(), pair.getName(), percChange.toPlainString(), prevBest.toPlainString());
				return false;
			}
		}
		return true;
	}

	
}
