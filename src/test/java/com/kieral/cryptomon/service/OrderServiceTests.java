package com.kieral.cryptomon.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangeService;
import com.kieral.cryptomon.service.exchange.TradingStatusListener;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class OrderServiceTests {

	static String BIT = "bittrex";
	static String POL = "poloniex";
	static String GDAX = "gdax";
			
	OrderService orderService;
	ExchangeManagerService exchangeManagerService;
	ExchangeService bittrex;
	ExchangeService poloniex;
	ExchangeService gdax;
	
	AtomicReference<OrderStatus> desiredStatus = new AtomicReference<OrderStatus>();
	AtomicReference<String> desiredExchangeOrderId = new AtomicReference<String>();
	Map<String, OrderStatus> desiredExchangeStatuses = new HashMap<String, OrderStatus>();
	List<Order> desiredExchangeOpenOrders = new ArrayList<Order>();
	
	Map<String, TradingStatusListener> tradingStatusListeners = new HashMap<String, TradingStatusListener>();
	Map<String, ConnectionStatus> desiredExchangeStatus = new HashMap<String, ConnectionStatus>();
	Map<String, Boolean> desiredExchangeTradingEnabled = new HashMap<String, Boolean>();

	BlockingQueue<Order> orderUpdates = new ArrayBlockingQueue<Order>(512);
	
	@Test
	public void testRequestsOpenOrdersOnTradingEnabled() {
		assertTrue(tradingStatusListeners.containsKey(BIT));
		assertTrue(tradingStatusListeners.containsKey(POL));
		tradingStatusListeners.get(BIT).onTradingEnabled(false);
		Mockito.verify(bittrex, Mockito.after(100).never()).getOpenOrders();
		tradingStatusListeners.get(POL).onTradingEnabled(false);
		Mockito.verify(poloniex, Mockito.after(100).never()).getOpenOrders();
		tradingStatusListeners.get(BIT).onTradingEnabled(true);
		Mockito.verify(bittrex, Mockito.after(100).atLeastOnce()).getOpenOrders();
		Mockito.verify(bittrex, Mockito.after(100).atMost(1)).getOpenOrders();
		Mockito.verify(poloniex, Mockito.after(100).never()).getOpenOrders();
		tradingStatusListeners.get(POL).onTradingEnabled(true);
		Mockito.verify(poloniex, Mockito.after(100).atLeastOnce()).getOpenOrders();
		Mockito.verify(poloniex, Mockito.after(100).atMost(1)).getOpenOrders();
	}

	@Test
	public void failedPlaceOrderPropagatesCancelStatus() throws InterruptedException {
		desiredStatus.set(OrderStatus.CANCELLED);
		orderService.placeOrder(order(BIT));
		Order order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
	}
	
	@Test
	public void placeOrderPropagatesOpenStatus() throws InterruptedException {
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set("AAA");
		orderService.placeOrder(order(BIT));
		Order order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNotNull(order.getOrderId());
		assertEquals(OrderStatus.OPEN, order.getOrderStatus());
		assertEquals("AAA", order.getOrderId());
	}

	@Test
	public void checkOrderPropagatesNewStatus() throws InterruptedException, OrderNotExistsException {
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set("AAA");
		orderService.placeOrder(order(BIT));
		Order order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNotNull(order.getOrderId());
		assertEquals(OrderStatus.OPEN, order.getOrderStatus());
		assertEquals("AAA", order.getOrderId());
		desiredExchangeStatuses.put(order.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		orderService.checkStatus(BIT, order.getClientOrderId());
		Order updatedOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertNotNull(updatedOrder);
		assertEquals(order.getClientOrderId(), updatedOrder.getClientOrderId());
		assertEquals(order.getOrderId(), updatedOrder.getOrderId());
		assertEquals(OrderStatus.PARTIALLY_FILLED, updatedOrder.getOrderStatus());
	}

	@Test
	public void failedOrderDetectsOpen() throws InterruptedException {
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set("AAA");
		orderService.placeOrder(order(BIT));
		Order firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set(null);
		Order exchangeOrder1 = order(BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		Order exchangeOrder2 = order(BIT, "LTCBTC");
		exchangeOrder2.setOrderId("BBB");
		exchangeOrder2.setOrderStatus(OrderStatus.OPEN);
		desiredExchangeOpenOrders.add(exchangeOrder1);
		desiredExchangeOpenOrders.add(exchangeOrder2);
		orderService.placeOrder(order(BIT, "LTCBTC"));
		Order secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.OPEN, secondOrder.getOrderStatus());
		assertEquals("BBB", secondOrder.getOrderId());
	}

	@Test
	public void failedOrderDetectsCancelled() throws InterruptedException {
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set("AAA");
		orderService.placeOrder(order(BIT));
		Order firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set(null);
		Order exchangeOrder1 = order(BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		desiredExchangeOpenOrders.add(exchangeOrder1);
		orderService.placeOrder(order(BIT, "LTCBTC"));
		Order secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.CANCELLED, secondOrder.getOrderStatus());
		assertNull(secondOrder.getOrderId());
	}

	@Test
	public void failedOrderDoesntAssumeWrongOrphanMeansIsOpen() throws InterruptedException {
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set("AAA");
		orderService.placeOrder(order(BIT));
		Order firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		desiredStatus.set(OrderStatus.OPEN);
		desiredExchangeOrderId.set(null);
		Order exchangeOrder1 = order(BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		Order exchangeOrder2 = order(POL, "LTCBTC", "0.01", "0.123567", false);
		exchangeOrder2.setOrderId("BBB");
		exchangeOrder2.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder3 = order(POL, "LTCBTC", "0.01", "0.123568", true);
		exchangeOrder3.setOrderId("BBB");
		exchangeOrder3.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder4 = order(POL, "LTCBTC", "0.02", "0.123567", true);
		exchangeOrder4.setOrderId("BBB");
		exchangeOrder4.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder5 = order(POL, "ETHBTC", "0.01", "0.123567", true);
		exchangeOrder5.setOrderId("BBB");
		exchangeOrder5.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder6 = order(BIT, "LTCBTC", "0.01", "0.123567", true);
		exchangeOrder6.setOrderId("BBB");
		exchangeOrder6.setOrderStatus(OrderStatus.OPEN);
		desiredExchangeOpenOrders.add(exchangeOrder1);
		desiredExchangeOpenOrders.add(exchangeOrder2);
		desiredExchangeOpenOrders.add(exchangeOrder3);
		desiredExchangeOpenOrders.add(exchangeOrder4);
		desiredExchangeOpenOrders.add(exchangeOrder5);
		desiredExchangeOpenOrders.add(exchangeOrder6);
		orderService.placeOrder(order(POL, "LTCBTC", "0.01", "0.123567", true));
		Order secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = orderUpdates.poll(1000, TimeUnit.MILLISECONDS);
		assertEquals(OrderStatus.CANCELLED, secondOrder.getOrderStatus());
		assertNull(secondOrder.getOrderId());
	}

	private Order order(String market) {
		return order(market, null);
	}

	private Order order(String market, String pair) {
		return order(market, pair, null, null, true);
	}

	private Order order(String market, String pair, String amount, String price, boolean side) {
		final Order order = new Order(market, pair == null ? "ETHBTC" : pair, amount == null ? new BigDecimal("0.001") : new BigDecimal(amount),
				price == null ? new BigDecimal("0.13875") : new BigDecimal(price), side == true ? Side.BID : Side.ASK);
		order.setCurrencyPair(TestUtils.cpFor(order.getCurrencyPairStr()));
		return order;
	}
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUo() {
		bittrex = Mockito.mock(ExchangeService.class);
		Mockito.when(bittrex.getName()).thenReturn(BIT);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(BIT, invocation.getArgument(0));
				return null;
			}}).when(bittrex)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		poloniex = Mockito.mock(ExchangeService.class);
		Mockito.when(poloniex.getName()).thenReturn(POL);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(POL, invocation.getArgument(0));
				return null;
			}}).when(poloniex)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		gdax = Mockito.mock(ExchangeService.class);
		Mockito.when(gdax.getName()).thenReturn(GDAX);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(GDAX, invocation.getArgument(0));
				return null;
			}}).when(gdax)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		List<ExchangeService> services = Arrays.asList(new ExchangeService[] {bittrex, poloniex, gdax});
		exchangeManagerService = Mockito.mock(ExchangeManagerService.class);
		Mockito.when(exchangeManagerService.getEnabledExchanges()).thenReturn(services);
		List<String> serviceNames = services.stream().map(ExchangeService::getName).collect(Collectors.<String>toList());
		Mockito.when(exchangeManagerService.getEnabledExchangeNames()).thenReturn(serviceNames);
		orderService = new OrderService();
		Mockito.doAnswer(new Answer<OrderStatus>() {
			@Override
			public OrderStatus answer(InvocationOnMock invocation) throws Throwable {
				if (OrderStatus.CANCELLED != desiredStatus.get()) {
					((Order)invocation.getArgument(0)).setOrderId(desiredExchangeOrderId.get());
				}
				return desiredStatus.get();
			}}).when(exchangeManagerService).placeOrder(Mockito.any(Order.class));
		Mockito.doAnswer(new Answer<Map<String, OrderStatus>>(){
			@Override
			public Map<String, OrderStatus> answer(InvocationOnMock invocation) throws Throwable {
				return desiredExchangeStatuses;
			}}).when(exchangeManagerService)
			.getOpenOrderStatuses(Mockito.anyString(), Mockito.any(List.class));
		Mockito.doAnswer(new Answer<List<Order>>(){
			@Override
			public List<Order> answer(InvocationOnMock invocation) throws Throwable {
				return desiredExchangeOpenOrders;
			}}).when(exchangeManagerService).getOpenOrders(Mockito.anyString());
		ReflectionTestUtils.setField(orderService, "exchangeManagerService", exchangeManagerService);
		desiredExchangeStatus.put(BIT, ConnectionStatus.CONNECTED);
		desiredExchangeStatus.put(POL, ConnectionStatus.CONNECTED);
		desiredExchangeStatus.put(GDAX, ConnectionStatus.DISCONNECTED);
		desiredExchangeTradingEnabled.put(BIT, false);
		desiredExchangeTradingEnabled.put(POL, false);
		desiredExchangeTradingEnabled.put(GDAX, false);
		orderService.registerOrderListener(order -> {
			try {
				orderUpdates.put(new Order(order));
			} catch (Exception e) {
				e.printStackTrace();
				fail("Cant place order " + order + " on test result queue " + e.getMessage());
			}
		});
		orderService.init();
	}
}
