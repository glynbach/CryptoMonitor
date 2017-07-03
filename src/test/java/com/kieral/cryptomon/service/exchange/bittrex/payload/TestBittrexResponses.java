package com.kieral.cryptomon.service.exchange.bittrex.payload;

import static org.junit.Assert.*;

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
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.rest.OrderResponse.RequestNature;

public class TestBittrexResponses {

	private static ObjectMapper objectMapper = new ObjectMapper();
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Running " + description.getMethodName());
	   }
	};

	@Test
	public void testEmptyOpenOrders() throws JsonParseException, JsonMappingException, IOException {
		BittrexOrdersResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/emptyOpenOrders.json"), BittrexOrdersResponseContainer.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
	}
	
	@Test
	public void testClosedOrderResponse() throws JsonParseException, JsonMappingException, IOException {
		BittrexOrderResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/closedOrderResponse.json"), BittrexOrderResponseContainer.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertEquals("301c93db-3193-46c6-93c6-33f995173ac4", response.getOrderId());
		assertEquals(Side.BID, response.getSide());
		assertEquals("0.01000000", response.getAmount().toPlainString());
		assertEquals("0.13750000", response.getPrice().toPlainString());
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(RequestNature.CLOSED_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	@Test
	public void testPlaceOrderFailure() throws JsonParseException, JsonMappingException, IOException {
		BittrexActionResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/placeOrderFailure.json"), BittrexActionResponseContainer.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(PlaceOrderResponse.class, OrderStatus.SENDING));
		assertEquals("DUST_TRADE_DISALLOWED_MIN_VALUE_50K_SAT", response.getMessage());
	}

	@Test
	public void testPlaceOrderSuccess() throws JsonParseException, JsonMappingException, IOException {
		BittrexActionResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/placeOrderSuccess.json"), BittrexActionResponseContainer.class);
		System.out.println(response);
		assertEquals(OrderStatus.OPEN, response.getOrderStatus(PlaceOrderResponse.class, OrderStatus.SENDING));
		assertEquals("96eb6d8e-bc4a-4bd5-a59d-d05c603ad655", response.getOrderId());
	}

	@Test
	public void testRequestOrderHistory() throws JsonParseException, JsonMappingException, IOException {
		BittrexOrdersResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestOrderHistory.json"), BittrexOrdersResponseContainer.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getOrderResponses());
		assertEquals(1, response.getOrderResponses().size());
		assertEquals("e5f8058b-dc19-454c-b18e-a178c27db9d2", response.getOrderResponses().get(0).getOrderId());
		assertEquals(Side.BID, response.getOrderResponses().get(0).getSide());
		assertEquals("0.01000000", response.getOrderResponses().get(0).getAmount().toPlainString());
		assertEquals("0.13900100", response.getOrderResponses().get(0).getPrice().toPlainString());
		assertEquals(OrderStatus.FILLED, response.getOrderResponses().get(0).getOrderStatus(RequestNature.CLOSED_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	@Test
	public void testRequestBalances() throws JsonParseException, JsonMappingException, IOException {
		BittrexAccountsResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestBalances.json"), BittrexAccountsResponseContainer.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getAccountResponses());
		assertNotNull(ar("BTC", response.getAccountResponses()));
		assertEquals("0.49860652", ar("BTC", response.getAccountResponses()).getAvailableBalance().toPlainString());
		assertNotNull(ar("ETH", response.getAccountResponses()));
		assertEquals("0.01000000", ar("ETH", response.getAccountResponses()).getAvailableBalance().toPlainString());
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
		BittrexActionResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestCancelOrderAlreadyClosed.json"), BittrexActionResponseContainer.class);
		System.out.println(response);
		assertEquals(OrderStatus.ERROR, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
		assertEquals("ORDER_NOT_OPEN", response.getExchangeMessage());
	}

	@Test
	public void testRequestCancelOrderNotExists() throws JsonParseException, JsonMappingException, IOException {
		BittrexActionResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestCancelOrderNotExists.json"), BittrexActionResponseContainer.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
		assertEquals("UUID_INVALID", response.getExchangeMessage());
	}

	@Test
	public void testRequestCancelSuccess() throws JsonParseException, JsonMappingException, IOException {
		BittrexActionResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestCancelSuccess.json"), BittrexActionResponseContainer.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
	}

	@Test
	public void testRequestOpenOrdersExists() throws JsonParseException, JsonMappingException, IOException {
		BittrexOrderResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestOpenOrdersExists.json"), BittrexOrderResponseContainer.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertEquals("96eb6d8e-bc4a-4bd5-a59d-d05c603ad655", response.getOrderId());
		assertEquals(Side.BID, response.getSide());
		assertEquals("0.01000000", response.getAmount().toPlainString());
		assertEquals("0.13800000", response.getPrice().toPlainString());
		assertEquals(OrderStatus.OPEN, response.getOrderStatus(RequestNature.OPEN_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	@Test
	public void testRequestOrderNotExists() throws JsonParseException, JsonMappingException, IOException {
		BittrexOrderResponseContainer response = objectMapper.readValue(this.getClass().getResourceAsStream("/bittrex/requestOrderNotExists.json"), BittrexOrderResponseContainer.class);
		System.out.println(response);
		assertFalse(response.isSuccess());
		assertEquals("UUID_INVALID", response.getMessage());
	}

	private Order orderFor(final String amount) {
		Order order = Mockito.mock(Order.class);
		Mockito.when(order.getAmount()).thenReturn(new BigDecimal(amount));
		return order;
	}

}
