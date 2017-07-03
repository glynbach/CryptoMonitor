package com.kieral.cryptomon.service.exchange.gdax.payload;

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
import com.kieral.cryptomon.service.rest.PlaceOrderResponse;
import com.kieral.cryptomon.service.rest.OrderResponse.RequestNature;

public class TestGdaxResponses {

	private static ObjectMapper objectMapper = new ObjectMapper();
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Running " + description.getMethodName());
	   }
	};

	@Test
	public void testEmptyOpenOrders() throws JsonParseException, JsonMappingException, IOException {
		GdaxOpenOrdersResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/emptyOpenOrders.json"), GdaxOpenOrdersResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
	}
	
	@Test
	public void testFilledOrderResponse() throws JsonParseException, JsonMappingException, IOException {
		GdaxFillsResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/filledOrderResponse.json"), GdaxFillsResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getOrderResponses());
		assertEquals(1, response.getOrderResponses().size());
		assertEquals("bb4ef795-2be9-4d20-9f5c-549de0a5b3ec", response.getOrderResponses().get(0).getOrderId());
		assertEquals(OrderStatus.FILLED, response.getOrderResponses().get(0).getOrderStatus(RequestNature.CLOSED_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	@Test
	public void testPlaceOrderSuccess() throws JsonParseException, JsonMappingException, IOException {
		GdaxPlaceOrderResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/placeOrderSuccess.json"), GdaxPlaceOrderResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.OPEN, response.getOrderStatus(PlaceOrderResponse.class, OrderStatus.SENDING));
		assertEquals("3d0fb74e-e4b5-43b5-98cd-a79cdfb13810", response.getOrderId());
	}

	@Test
	public void testRequestBalances() throws JsonParseException, JsonMappingException, IOException {
		GdaxAccountsResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/requestBalances.json"), GdaxAccountsResponse.class);
		System.out.println(response);
		assertNotNull(response.getAccountResponses());
		assertNotNull(ar("BTC", response.getAccountResponses()));
		assertEquals("0.4986067327000000", ar("BTC", response.getAccountResponses()).getAvailableBalance().toPlainString());
		assertNotNull(ar("ETH", response.getAccountResponses()));
		assertEquals("0.0100000000000000", ar("ETH", response.getAccountResponses()).getAvailableBalance().toPlainString());
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
	public void testRequestCancelSuccess() throws JsonParseException, JsonMappingException, IOException {
		GdaxCancelOrderResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/requestCancelSuccess.json"), GdaxCancelOrderResponse.class);
		System.out.println(response);
		assertEquals(OrderStatus.CANCELLED, response.getOrderStatus(CancelOrderResponse.class, OrderStatus.OPEN));
	}

	@Test
	public void testRequestOpenOrdersExists() throws JsonParseException, JsonMappingException, IOException {
		GdaxOpenOrdersResponse response = objectMapper.readValue(this.getClass().getResourceAsStream("/gdax/requestOpenOrdersExists.json"), GdaxOpenOrdersResponse.class);
		System.out.println(response);
		assertTrue(response.isSuccess());
		assertNotNull(response.getOrderResponses());
		assertEquals(1, response.getOrderResponses().size());
		assertEquals("3d0fb74e-e4b5-43b5-98cd-a79cdfb13810", response.getOrderResponses().get(0).getOrderId());
		assertEquals(Side.BID, response.getOrderResponses().get(0).getSide());
		assertEquals("0.01000000", response.getOrderResponses().get(0).getAmount().toPlainString());
		assertEquals("0.13750000", response.getOrderResponses().get(0).getPrice().toPlainString());
		assertEquals(OrderStatus.OPEN, response.getOrderResponses().get(0).getOrderStatus(RequestNature.OPEN_ORDER_RESPONSE, orderFor("0.01000000")));
	}

	private Order orderFor(final String amount) {
		Order order = Mockito.mock(Order.class);
		Mockito.when(order.getAmount()).thenReturn(new BigDecimal(amount));
		return order;
	}

}
