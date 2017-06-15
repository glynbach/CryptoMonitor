package com.kieral.cryptomon.tickstore;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Repository;

import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.tickstore.model.OrderBookDataEntry;

@Transactional
@Repository
public class OrderBookDaoImpl implements OrderBookDao {

	private final static String sessionId = UUID.randomUUID().toString();
	
	@PersistenceContext	
	private EntityManager entityManager;		

	@Override
	public void saveOrderBookDataEntries(OrderBook orderBook) {
		if (orderBook == null)
			return;
		String market = orderBook.getMarket();
		String ccyPair = orderBook.getCurrencyPair().getName();
		long snapshotReceived = orderBook.getSnapshotReceived();
		String baseCurrency = orderBook.getCurrencyPair().getBaseCurrency().name();
		String quotedCurrency = orderBook.getCurrencyPair().getQuotedCurrency().name();
		String topic = orderBook.getCurrencyPair().getTopic();
		BigDecimal tradingFee = orderBook.getCurrencyPair().getTradingFee();
		long snapshotTime = orderBook.getSnapshotReceived();
		if (orderBook.getBids() != null) {
			String side = "BID";
			final AtomicInteger index = new AtomicInteger(0);
			orderBook.getBids().forEach(bid -> {
				OrderBookDataEntry dataEntry = new OrderBookDataEntry(sessionId, market, ccyPair, snapshotReceived,
						side, index.getAndIncrement(), baseCurrency, quotedCurrency, topic, bid.getPrice(), 
						bid.getAmount(), tradingFee, null, snapshotTime);
				entityManager.persist(dataEntry);
			});
		}
		if (orderBook.getAsks() != null) {
			String side = "ASK";
			final AtomicInteger index = new AtomicInteger(0);
			orderBook.getAsks().forEach(ask -> {
				OrderBookDataEntry dataEntry = new OrderBookDataEntry(sessionId, market, ccyPair, snapshotReceived,
						side, index.getAndIncrement(), baseCurrency, quotedCurrency, topic, ask.getPrice(), 
						ask.getAmount(), tradingFee, null, snapshotTime);
				entityManager.persist(dataEntry);
			});
		}
	}
}
