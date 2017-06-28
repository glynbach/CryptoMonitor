package com.kieral.cryptomon.model.trading;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class OrderTests {

	ServiceExchangeProperties exchangeProperties;
	List<CurrencyPair> pairs;
	
	@Before
	public void setUp() {
		exchangeProperties = Mockito.mock(ServiceExchangeProperties.class);
		Mockito.when(exchangeProperties.getPairs()).thenReturn(pairs);
		Mockito.doAnswer(new Answer<CurrencyPair>(){
			@Override
			public CurrencyPair answer(InvocationOnMock invocation) throws Throwable {
				String name = invocation.getArgument(0);
				for (CurrencyPair pair : pairs) {
					if (pair.getName().equals(name))
						return pair;
				}
				return null;
			}}).when(exchangeProperties).getPair(Mockito.anyString());
		pairs = TestUtils.createTestPairsList();
	}
	
	@Test
	public void testOrderCreate() {
		Order order = orderFrom("bittrex", "ETHBTC", "0.01", "0.13958001", false);
		assertEquals("bittrex", order.getMarket());
		assertEquals("ETHBTC", order.getCurrencyPair().getName());
		TestUtils.assertEquals("0.01", order.getAmount());
		TestUtils.assertEquals("0.13958001", order.getPrice());
		assertNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertNull(order.getOrderStatus());
		order.setOrderId("AAA");
		order.setClientOrderId("BBB");
		order.setOrderStatus(OrderStatus.OPEN);
		assertEquals("AAA", order.getOrderId());
		assertEquals("BBB", order.getClientOrderId());
		assertEquals(OrderStatus.OPEN, order.getOrderStatus());
	}
	
	@Test 
	public void testReplaceTrades() {
		Order order = orderFrom("bittrex", "ETHBTC", "0.01", "0.13958001", false);
		order.replaceTrades(tradesFrom(tradeFrom(null, "0.13958002", "0.005")));
		assertNotNull(order.getTrades());
		assertEquals(1, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		order.replaceTrades(tradesFrom(tradeFrom(null, "0.13958002", "0.01")));
		assertEquals(1, order.getTrades().size());
		TestUtils.assertEquals("0.01", order.getTrades().get(0).getAmount());
	}

	@Test 
	public void testMergeTradesReplacesPlaceholders() {
		Order order = orderFrom("poloniex", "ETHBTC", "0.01", "0.13958001", false);
		order.replaceTrades(tradesFrom(tradeFrom(null, "0.13958002", "0.005")));
		assertNotNull(order.getTrades());
		assertEquals(1, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		order.mergeTrades(tradesFrom(
				tradeFrom("Tr-1", "0.13958002", "0.005"),
				tradeFrom("Tr-2", "0.13958002", "0.004"),
				tradeFrom("Tr-3", "0.13958001", "0.001")));
		assertEquals(3, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		TestUtils.assertEquals("0.004", order.getTrades().get(1).getAmount());
		TestUtils.assertEquals("0.001", order.getTrades().get(2).getAmount());
	}

	@Test 
	public void testMergeTradesMerges() {
		Order order = orderFrom("poloniex", "ETHBTC", "0.01", "0.13958001", false);
		order.replaceTrades(tradesFrom(tradeFrom(null, "0.13958002", "0.005")));
		assertNotNull(order.getTrades());
		assertEquals(1, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		order.mergeTrades(tradesFrom(
				tradeFrom("Tr-1", "0.13958002", "0.005"),
				tradeFrom("Tr-2", "0.13958002", "0.004")));
		assertEquals(2, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		TestUtils.assertEquals("0.004", order.getTrades().get(1).getAmount());
		order.mergeTrades(tradesFrom(
				tradeFrom("Tr-1", "0.13958002", "0.005"),
				tradeFrom("Tr-2", "0.13958002", "0.004"),
				tradeFrom("Tr-3", "0.13958001", "0.001")));
		assertEquals(3, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		TestUtils.assertEquals("0.004", order.getTrades().get(1).getAmount());
		TestUtils.assertEquals("0.001", order.getTrades().get(2).getAmount());
	}

	@Test(expected=IllegalStateException.class) 
	public void testMergingWithAPlaceholderErrors() {
		Order order = orderFrom("poloniex", "ETHBTC", "0.01", "0.13958001", false);
		order.replaceTrades(tradesFrom(tradeFrom(null, "0.13958002", "0.005")));
		assertNotNull(order.getTrades());
		assertEquals(1, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		order.mergeTrades(tradesFrom(
				tradeFrom("Tr-1", "0.13958002", "0.005"),
				tradeFrom("Tr-2", "0.13958002", "0.004"),
				tradeFrom("Tr-3", "0.13958001", "0.001")));
		assertEquals(3, order.getTrades().size());
		TestUtils.assertEquals("0.005", order.getTrades().get(0).getAmount());
		TestUtils.assertEquals("0.004", order.getTrades().get(1).getAmount());
		TestUtils.assertEquals("0.001", order.getTrades().get(2).getAmount());
		order.mergeTrades(tradesFrom(
				tradeFrom(null, "0.13958001", "0.01")));
		fail("Expected error on illegal operation");
	}

	private Order orderFrom(String market, String pair, String amount, String price, boolean buy) {
		Order order = new Order(market, pair, new BigDecimal(amount), new BigDecimal(price), buy ? Side.BID : Side.ASK);
		order.setCurrencyPair(exchangeProperties.getPair(pair));
		return order;
	}

	private List<Trade> tradesFrom(Trade... trades) {
		return Arrays.asList(trades);
	}
	
	private Trade tradeFrom(String tradeId, String rate, String amount) {
		return new Trade(tradeId, new BigDecimal(rate), new BigDecimal(amount), BigDecimal.ZERO, true,
				System.currentTimeMillis());
	}
}

