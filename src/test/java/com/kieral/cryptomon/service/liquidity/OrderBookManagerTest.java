package com.kieral.cryptomon.service.liquidity;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.orderbook.OrderBookAction;
import com.kieral.cryptomon.model.orderbook.OrderBookUpdate;
import com.kieral.cryptomon.service.rest.OrderBookResponse;
import com.kieral.cryptomon.service.rest.OrderBookResponseEntry;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class OrderBookManagerTest {

    Function<OrderBookResponseEntry, OrderBookUpdate> updateFromBidEntries = new Function<OrderBookResponseEntry, OrderBookUpdate>() {
    	public OrderBookUpdate apply(OrderBookResponseEntry entry) {
    		return new OrderBookUpdate(Side.BID, entry.getPrice(), entry.getAmount(), OrderBookAction.REPLACE);
    	}
    };
    Function<OrderBookResponseEntry, OrderBookUpdate> updateFromAskEntries = new Function<OrderBookResponseEntry, OrderBookUpdate>() {
    	public OrderBookUpdate apply(OrderBookResponseEntry entry) {
    		return new OrderBookUpdate(Side.ASK, entry.getPrice(), entry.getAmount(), OrderBookAction.REPLACE);
    	}
    };

	OrderBookConfig orderBookConfig;
	OrderBookSanityChecker orderBookSanityChecker;
	OrderBookManager orderBookManager;
	
	@Before
	public void init() {
		orderBookConfig = new MockOrderBookConfig();
		orderBookSanityChecker = new OrderBookSanityCheckerImpl();
		orderBookManager = new OrderBookManager();
		ReflectionTestUtils.setField(orderBookManager, "orderBookConfig", orderBookConfig);
		ReflectionTestUtils.setField(orderBookManager, "orderBookSanityChecker", orderBookSanityChecker);
	}
	
	@Test
	public void testExportedOrderBookUpdates() throws IOException {
		CurrencyPair ethBtc = TestUtils.cpFor("ETHBTC");
		CurrencyPair ltcBtc = TestUtils.cpFor("LTCBTC");
		checkResponses("gdax", ethBtc, TestUtils.convertGdaxLogs(ethBtc, this.getClass().getResourceAsStream("/gdaxOrderbooksETHBTC.log"))); 
		checkResponses("gdax", ltcBtc, TestUtils.convertGdaxLogs(ethBtc, this.getClass().getResourceAsStream("/gdaxOrderbooksLTCBTC.log"))); 
		checkResponses("poloniex", ethBtc, TestUtils.convertPoloniexLogs(ethBtc, this.getClass().getResourceAsStream("/poloniexOrderbooksETHBTC.log"))); 
		checkResponses("poloniex", ltcBtc, TestUtils.convertPoloniexLogs(ethBtc, this.getClass().getResourceAsStream("/poloniexOrderbooksLTCBTC.log"))); 
		checkResponses("bittrex", ethBtc, TestUtils.convertBittrexLogs(ethBtc, this.getClass().getResourceAsStream("/bittrexOrderbooksETHBTC.log"))); 
		checkResponses("bittrex", ltcBtc, TestUtils.convertBittrexLogs(ethBtc, this.getClass().getResourceAsStream("/bittrexOrderbooksLTCBTC.log"))); 
	}
	
	private void checkResponses(String market, CurrencyPair pair, List<? extends OrderBookResponse> responses) {
		if (responses == null || responses.size() == 0)
			fail("Got no responses for " + market + " and " + pair.getName());
		for (OrderBookResponse response : responses) {
			orderBookManager.clearOrderBook(market, pair);
			orderBookManager.updateOrderBook(market, pair, response.getBidEntries().stream().map(updateFromBidEntries).collect(Collectors.toList()), 
					response.getSequence(), response.getCreatedTime(), 
					true, 10);
			OrderBook orderBook = orderBookManager.updateOrderBook(market, pair, response.getAskEntries().stream().map(updateFromAskEntries).collect(Collectors.toList()), 
					response.getSequence(), response.getCreatedTime(), 
					true, 10);
			assertTrue("Invalid order book " + orderBook, orderBook.isValid());
			assertNotNull(orderBook.getBids());
			assertTrue(orderBook.getBids().size() > 0);
			assertEquals(10, orderBook.getBids().size());
			for (int i=0; i<orderBook.getBids().size(); i++) {
				TestUtils.assertEquals(response.getBidEntries().get(i).getPrice(), orderBook.getBids().get(i).getPrice()); 
				TestUtils.assertEquals(response.getBidEntries().get(i).getAmount(), orderBook.getBids().get(i).getAmount()); 
			}
		}
	}

}
