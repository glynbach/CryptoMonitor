package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

public class ArbMonitorService implements ArbService {

	private final static ArbComparator COMPARATOR = new ArbComparator();

	@Autowired
	ExchangeManagerService exchangeManager;
	@Autowired
	BalanceService balanceHandler;
	@Autowired
	ArbInspector arbInspector;
	@Autowired
	ArbInstructionHandler arbInstructionHandler;
	
	private final ConcurrentMap<String, ConcurrentMap<String, OrderBook>> books = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBook>>();
	private final ConcurrentMap<String, List<String>> comparedBooks = new ConcurrentHashMap<String, List<String>>();
	private final AtomicBoolean suspended = new AtomicBoolean(false);
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@PostConstruct
	public void init() {
		exchangeManager.getEnabledExchanges().forEach(service -> {
			service.registerOrderBookListener(orderBook -> {
				processOrderBook(orderBook);
			});
			exchangeManager.registerConnectionStatusListener(service.getName(), status -> {
				processStatus(service.getName(), status);
			});
		});
	}

	@Override
	public OrderBook getOrderBook(String market, String currencyPairName) {
		return books.containsKey(market) ? books.get(market).get(currencyPairName) : null;
	}

	private void processOrderBook(OrderBook orderBook) {
		if (orderBook == null)
			return;
		if (logger.isDebugEnabled())
			logger.debug("processing orderBook {}", orderBook);
		books.putIfAbsent(orderBook.getMarket(), new ConcurrentHashMap<String, OrderBook>());
		OrderBook prevOrderBook = books.get(orderBook.getMarket()).put(orderBook.getCurrencyPair().getName(), orderBook);
		if (prevOrderBook != null)
			clearCompared(prevOrderBook);
		sweepBooks();
	}

	private void processStatus(String market, ConnectionStatus status) {
		if (status == ConnectionStatus.DISCONNECTED) {
			if (books.containsKey(market))
				books.get(market).clear();
		}
	}
	
	private void sweepBooks() {
		if (!suspended.get()) {
			final List<ArbInstruction> instructions = new ArrayList<ArbInstruction>();
			if (books.size() > 1) {
				books.keySet().forEach(market -> {
					books.get(market).values().forEach(book -> {
						books.keySet().forEach(otherMarket -> {
							if (!otherMarket.equals(market)) {
								books.get(otherMarket).values().forEach(otherBook -> {
									if (otherBook.getCurrencyPair().getName().equals(book.getCurrencyPair().getName())) {
										if (!checkAndSetComparedBooks(book, otherBook)) {
											if (logger.isDebugEnabled())
												logger.debug("examining {} versus {}", book, otherBook);
											instructions.add(arbInspector.examine(book, otherBook));
										} 
									}
								});
							}
						});
					});
				});
			}
			instructions.sort(COMPARATOR);
			if (logger.isDebugEnabled())
				logger.debug("results of sweep {}", instructions);
			if (instructions.size() > 0 && instructions.get(0).getDecision() != ArbDecision.NOTHING_THERE) {
				BigDecimal usdValue = instructions.get(0).getEstimatedValue().multiply(new BigDecimal("2500")).setScale(4, RoundingMode.HALF_UP);
				logger.info("Best arb decision for ~ {} {} {}", usdValue, "USD", instructions.get(0));
				// check affect of transfer fees
				// check inflight statuses
				// check market conditions
				// TODO: handle sending multiple instructions
				arbInstructionHandler.onArbInstruction(instructions.get(0));
			}
		}
	}
	
	private boolean checkAndSetComparedBooks(OrderBook orderBook1, OrderBook orderBook2) {
		if (orderBook1 == null)
			throw new IllegalArgumentException("orderBook1 can not be null");
		if (orderBook2 == null)
			throw new IllegalArgumentException("orderBook2 can not be null");
		String key1 = orderBook1.getMarket() + ":" + orderBook1.getSnapshotSequence();
		String key2 = orderBook2.getMarket() + ":" + orderBook2.getSnapshotSequence();
		comparedBooks.putIfAbsent(key1, new ArrayList<String>());
		if (comparedBooks.get(key1).contains(key2)) {
			// check the other way round wasn't missed
			comparedBooks.putIfAbsent(key2,  new ArrayList<String>());
			if (!comparedBooks.get(key2).contains(key1)) {
				comparedBooks.get(key2).add(key1);
			}
			return true;
		} else {
			comparedBooks.get(key1).add(key2);
			comparedBooks.putIfAbsent(key2,  new ArrayList<String>());
			if (!comparedBooks.get(key2).contains(key1)) {
				comparedBooks.get(key2).add(key1);
				return false;
			}
			return true;
		}
	}
	
	private void clearCompared(OrderBook orderBook) {
		if (orderBook == null)
			throw new IllegalArgumentException("orderBook can not be null");
		comparedBooks.remove(orderBook.getSnapshotSequence());
	}

	@Override
	public ArbInstruction calculateArb(OrderBook longBook, OrderBook shortBook, BigDecimal longAmountRemaining,
			BigDecimal shortAmountRemaining) {
		return arbInspector.resolve(longBook, shortBook, longAmountRemaining, shortAmountRemaining);
	}

	@Override
	public void suspend(boolean suspend) {
		suspended.set(suspend);
	}

	@Override
	public boolean isSuspended() {
		return suspended.get();
	}

}
