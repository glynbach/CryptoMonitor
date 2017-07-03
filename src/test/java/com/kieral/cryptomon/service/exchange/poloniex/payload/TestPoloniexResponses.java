package com.kieral.cryptomon.service.exchange.poloniex.payload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.CancelOrderResponse;
import com.kieral.cryptomon.service.rest.OrderResponse.RequestNature;
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;

public class TestPoloniexResponses {

	private static ObjectMapper objectMapper = new ObjectMapper();
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Running " + description.getMethodName());
	   }
	};

	@Test
	public void testEmptyOpenOrders() throws JsonParseException, JsonMappingException, IOException {
		PoloniexOpenOrdersResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/emptyOpenOrders.json"), PoloniexOpenOrdersResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
	}
	
	@Test
	public void testPlaceOrderFailure() throws JsonParseException, JsonMappingException, IOException {
		PoloniexActionResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/placeOrderFailure.json"), PoloniexActionResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(PlaceOrderResponse.class, OrderStatus.SENDING));
		assertEquals("Nonce must be greater than 1497900264856. You provided 1497900264854.", response.getExchangeMessage());
	}

	@Test
	public void testPlaceOrderSuccess() throws JsonParseException, JsonMappingException, IOException {
		PoloniexActionResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/placeOrderSuccess.json"), PoloniexActionResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.OPEN, response.getOrderStatus(PlaceOrderResponse.class, OrderStatus.SENDING));
		assertEquals("305476436929", response.getOrderId());
	}

	@Test
	public void testRequestBalances() throws JsonParseException, JsonMappingException, IOException {
		PoloniexAccountsResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/requestBalances.json"), PoloniexAccountsResponse.class);
		System.out.println(response);
		assertNotNull(response.getAccountResponses());
		assertNotNull(ar("BTC", response.getAccountResponses()));
		assertEquals("0.50000000", ar("BTC", response.getAccountResponses()).getAvailableBalance().toPlainString());
		assertNotNull(ar("ETH", response.getAccountResponses()));
		assertEquals("0.00000000", ar("ETH", response.getAccountResponses()).getAvailableBalance().toPlainString());
	}

	private AccountResponse ar(String currency, List<? extends AccountResponse> list) {
		if (list == null)
			fail("No account responses");
		for (AccountResponse ar : list) {
			if (ar.getAccountCurrency().name().equalsIgnoreCase(currency))
				return ar;
		}
		return null;
	}
	
	@Test
	public void testRequestCancelOrderAlreadyClosed() throws JsonParseException, JsonMappingException, IOException {
		PoloniexActionResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/requestCancelOrderAlreadyClosed.json"), PoloniexActionResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.ERROR, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
		assertEquals("Invalid order number, or you are not the person who placed the order.", response.getExchangeMessage());
	}

	@Test
	public void testRequestCancelOrderNotExists() throws JsonParseException, JsonMappingException, IOException {
		PoloniexActionResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/requestCancelOrderNotExists.json"), PoloniexActionResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.ERROR, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
		assertEquals("Invalid order number, or you are not the person who placed the order.", response.getExchangeMessage());
	}

	@Test
	public void testRequestCancelSuccess() throws JsonParseException, JsonMappingException, IOException {
		PoloniexActionResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/requestCancelSuccess.json"), PoloniexActionResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
	}

	@Test
	public void testRequestOpenOrdersExists() throws JsonParseException, JsonMappingException, IOException {
		PoloniexOpenOrdersResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/requestOpenOrdersExists.json"), PoloniexOpenOrdersResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getOrderResponses());
		assertEquals(1, response.getOrderResponses().size());
		assertEquals("305444853544", response.getOrderResponses().get(0).getOrderId());
		assertEquals(Side.BID, response.getOrderResponses().get(0).getSide());
		assertEquals("0.01000000", response.getOrderResponses().get(0).getAmount().toPlainString());
		assertEquals("0.13750000", response.getOrderResponses().get(0).getPrice().toPlainString());
		assertEquals(OrderStatus.OPEN, response.getOrderResponses().get(0).getOrderStatus(RequestNature.OPEN_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	@Test
	public void testTradeHistoryResponse() throws JsonParseException, JsonMappingException, IOException {
		PoloniexOrderHistoryResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/poloniex/tradeHistoryResponse.json"), PoloniexOrderHistoryResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getOrderResponses());
		assertEquals(1, response.getOrderResponses().size());
		assertEquals("305476436929", response.getOrderResponses().get(0).getOrderId());
		assertEquals(OrderStatus.FILLED, response.getOrderResponses().get(0).getOrderStatus(RequestNature.CLOSED_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	private Order orderFor(final String amount) {
		Order order = Mockito.mock(Order.class);
		Mockito.when(order.getAmount()).thenReturn(new BigDecimal(amount));
		return order;
	}
}
