package com.kieral.cryptomon.service.arb.execution;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.MockOrderService;
import com.kieral.cryptomon.service.PollListener;
import com.kieral.cryptomon.service.PollingService;
import com.kieral.cryptomon.service.arb.ArbDecision;
import com.kieral.cryptomon.service.arb.ArbInspector;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstructionFactory;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.arb.SimpleArbInspector;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exception.NotEnoughFundsException;
import com.kieral.cryptomon.service.liquidity.MockOrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.Tuple2;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class TestExecutionMonitor {

	MockOrderService orderService;
	BalanceService balanceService;
	TestArbService arbService;
	BackOfficeService backOfficeService;
	TestPollingService pollingService;
	CountDownLatch testLatch = new CountDownLatch(1);
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Running " + description.getMethodName());
	   }
	};

	@SuppressWarnings("unchecked")
	@Before
	public void init() {
		OrderBookConfig orderBookConfig = new MockOrderBookConfig();
		OrderBookManager orderBookManager = new OrderBookManager();
		ReflectionTestUtils.setField(orderBookManager, "orderBookConfig", orderBookConfig);
		ArbInspector arbInspector = new SimpleArbInspector();
		ReflectionTestUtils.setField(arbInspector, "orderBookConfig", orderBookConfig);
		ReflectionTestUtils.setField(arbInspector, "orderBookManager", orderBookManager);
		balanceService = Mockito.mock(BalanceService.class);
		arbService = new TestArbService(arbInspector);
		orderService = new MockOrderService();
		orderService.setDesiredExchangeStatus(MockOrderService.BIT, ConnectionStatus.CONNECTED);
		orderService.setDesiredExchangeStatus(MockOrderService.POL, ConnectionStatus.CONNECTED);
		orderService.setDesiredExchangeStatus(MockOrderService.GDAX, ConnectionStatus.CONNECTED);
		orderService.setDesiredExchangeTradingEnabled(MockOrderService.BIT, true);
		orderService.setDesiredExchangeTradingEnabled(MockOrderService.POL, true);
		orderService.setDesiredExchangeTradingEnabled(MockOrderService.GDAX, true);
		backOfficeService = Mockito.mock(BackOfficeService.class);
		pollingService = new TestPollingService();
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				testLatch.countDown();
				return null;
			}}).when(backOfficeService).onExecutionCompletion(Mockito.any(ArbInstruction.class), 
				Mockito.any(List.class), Mockito.any(List.class), Mockito.anyBoolean());
	}
	
	@After
	public void tearDown() {
		pollingService.listeners.clear();
		arbService.suspend(true);
	}

	@Test
	public void testPlacesOrders() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		TestUtils.assertEquals("0.13400", longOrder.getPrice());
		TestUtils.assertEquals("0.5", longOrder.getAmount());
		assertEquals(MockOrderService.BIT, longOrder.getMarket());
		assertEquals(OrderStatus.OPEN, longOrder.getOrderStatus());
		TestUtils.assertEquals("0.13500", shortOrder.getPrice());
		TestUtils.assertEquals("0.5", shortOrder.getAmount());
		assertEquals(MockOrderService.POL, shortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, shortOrder.getOrderStatus());
	}
	
	@Test
	public void detectsDoneOrders() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.FILLED);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertTrue(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void resolvesNoFillsOrdersNoChange() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13300"}, new String[]{"3"}, new String[]{"0.13400"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13500"}, new String[]{"1"}, new String[]{"0.13600"}, new String[]{"2"}));
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNull(longOrder);
		assertNull(shortOrder);
		// No other orders placed
		assertEquals(2, orderService.getNumPlacedOrders());
	}

	@Test
	public void resolvesNoFillsOrdersPriceChange() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.5", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.5", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
	}

	@Test
	public void resolvesNoFillsOrdersPriceChangeUsesLowerBookLevel() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13320", "0.13310"}, new String[]{"0.1", "3"}, 
						new String[]{"0.13400", "0.13410"}, new String[]{"0.1", "5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13491", "0.13490"}, new String[]{"0.1", "1"}, 
						new String[]{"0.13589", "0.13590"}, new String[]{"0.1", "2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.5", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.5", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
	}

	@Test
	public void resolvesNothingThereCancelsOut() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13350"}, new String[]{"3"}, new String[]{"0.13450"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"1"}, new String[]{"0.13550"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNoMoreOrders(2, 100);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void resolvesPartialFillsOrdersPriceChange() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		assertTrue(longOrder.getAmount().compareTo(newLongOrder.getAmount()) > 0);
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		assertTrue(shortOrder.getAmount().compareTo(newShortOrder.getAmount()) > 0);
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
	}

	@Test
	public void resolvesPartialFillsCancelsNothingThere() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		assertTrue(longOrder.getAmount().compareTo(newLongOrder.getAmount()) > 0);
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		assertTrue(shortOrder.getAmount().compareTo(newShortOrder.getAmount()) > 0);
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"3"}, new String[]{"0.13550"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"1"}, new String[]{"0.13550"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredOrderStatus(newLongOrder.getClientOrderId(), OrderStatus.OPEN);
		orderService.setDesiredOrderStatus(newShortOrder.getClientOrderId(), OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNoMoreOrders(4, 100);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}
	
	@Test
	public void testMisMatchedPartialFillsBidFillsLargerBothOpenCanStillBalance() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.2"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.2", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.3", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"3"}, new String[]{"0.13550"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13410"}, new String[]{"1"}, new String[]{"0.13450"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredOrderStatus(newLongOrder.getClientOrderId(), OrderStatus.OPEN);
		orderService.setDesiredOrderStatus(newShortOrder.getClientOrderId(), OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(5, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		assertNotNull(balancingOrder);
		assertEquals(OrderStatus.SENDING, balancingOrder.getOrderStatus());
		balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		TestUtils.assertEquals("0.13410", balancingOrder.getPrice());
		TestUtils.assertEquals("0.1", balancingOrder.getAmount());
		assertEquals(Side.ASK, balancingOrder.getSide());
		assertEquals(MockOrderService.POL, balancingOrder.getMarket());
		awaitNoMoreOrders(5, 100);
	}

	@Test
	public void testMisMatchedPartialFillsBidFillsLargerBothOpenCannotStillBalance() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.2"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.2", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.3", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"3"}, new String[]{"0.13550"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13390"}, new String[]{"1"}, new String[]{"0.13490"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredOrderStatus(newLongOrder.getClientOrderId(), OrderStatus.OPEN);
		orderService.setDesiredOrderStatus(newShortOrder.getClientOrderId(), OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNoMoreOrders(4, 100);
	}

	@Test
	public void testMisMatchedPartialFillsAskFillsLargerBothOpenCanStillBalance() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.2"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.3", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.2", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"3"}, new String[]{"0.13490"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13410"}, new String[]{"1"}, new String[]{"0.13450"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredOrderStatus(newLongOrder.getClientOrderId(), OrderStatus.OPEN);
		orderService.setDesiredOrderStatus(newShortOrder.getClientOrderId(), OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(5, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		assertNotNull(balancingOrder);
		assertEquals(OrderStatus.SENDING, balancingOrder.getOrderStatus());
		balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", balancingOrder.getPrice());
		TestUtils.assertEquals("0.1", balancingOrder.getAmount());
		assertEquals(Side.BID, balancingOrder.getSide());
		assertEquals(MockOrderService.BIT, balancingOrder.getMarket());
		awaitNoMoreOrders(5, 100);
	}

	@Test
	public void testMisMatchedPartialFillsAskFillsLargerBothOpenCannotStillBalance() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.2"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(4, 100);
		Tuple2<Order, Order> newOrders = orderService.getLastOrders();
		pollForStatuses(newOrders.getA().getClientOrderId(), OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		pollForStatuses(newOrders.getB().getClientOrderId(), OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotEquals(longOrder.getClientOrderId(), newLongOrder.getClientOrderId());
		assertNotEquals(shortOrder.getClientOrderId(), newShortOrder.getClientOrderId());
		TestUtils.assertEquals("0.13410", newLongOrder.getPrice());
		TestUtils.assertEquals("0.3", newLongOrder.getAmount());
		assertEquals(MockOrderService.BIT, newLongOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newLongOrder.getOrderStatus());
		TestUtils.assertEquals("0.13490", newShortOrder.getPrice());
		TestUtils.assertEquals("0.2", newShortOrder.getAmount());
		assertEquals(MockOrderService.POL, newShortOrder.getMarket());
		assertEquals(OrderStatus.OPEN, newShortOrder.getOrderStatus());
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13500"}, new String[]{"3"}, new String[]{"0.13550"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13390"}, new String[]{"1"}, new String[]{"0.13490"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredOrderStatus(newLongOrder.getClientOrderId(), OrderStatus.OPEN);
		orderService.setDesiredOrderStatus(newShortOrder.getClientOrderId(), OrderStatus.OPEN);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(newOrders.getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(newOrders.getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNoMoreOrders(4, 100);
	}

	@Test
	public void testMisMatchedFillsBidFillsLargerBidSideClosed() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(3, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		pollForStatuses(balancingOrderRef.getClientOrderId(), OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		TestUtils.assertEquals("0.13490", balancingOrder.getPrice());
		TestUtils.assertEquals("0.2", balancingOrder.getAmount());
		assertEquals(MockOrderService.POL, balancingOrder.getMarket());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		assertEquals(Side.ASK, balancingOrder.getSide());
		awaitNoMoreOrders(3, 100);
	}

	@Test
	public void testMisMatchedFillsBidFillsLargerBidSideClosed2() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13410"}, new String[]{"3"}, new String[]{"0.13510"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNumOrders(3, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		pollForStatuses(balancingOrderRef.getClientOrderId(), OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		TestUtils.assertEquals("0.13490", balancingOrder.getPrice());
		TestUtils.assertEquals("0.2", balancingOrder.getAmount());
		assertEquals(MockOrderService.POL, balancingOrder.getMarket());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		assertEquals(Side.ASK, balancingOrder.getSide());
		awaitNoMoreOrders(3, 100);
	}

	@Test
	public void testMisMatchedFillsBidFillsLargerBidSideClosedNothingThere() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13410"}, new String[]{"3"}, new String[]{"0.13510"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13390"}, new String[]{"1"}, new String[]{"0.13490"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		awaitNoMoreOrders(2, 100);
	}

	@Test
	public void testMisMatchedFillsAskFillsLargerAskSideClosed() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.FILLED);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		awaitNumOrders(3, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		pollForStatuses(balancingOrderRef.getClientOrderId(), OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		TestUtils.assertEquals("0.13410", balancingOrder.getPrice());
		TestUtils.assertEquals("0.2", balancingOrder.getAmount());
		assertEquals(MockOrderService.BIT, balancingOrder.getMarket());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		assertEquals(Side.BID, balancingOrder.getSide());
		awaitNoMoreOrders(3, 100);
	}

	@Test
	public void testMisMatchedFillsAskFillsLargerAskSideClosed2() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13410"}, new String[]{"3"}, new String[]{"0.13490"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.FILLED);
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		awaitNumOrders(3, 100);
		Order balancingOrderRef = orderService.getLastOrder();
		pollForStatuses(balancingOrderRef.getClientOrderId(), OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(balancingOrderRef.getClientOrderId(), 100);
		TestUtils.assertEquals("0.13490", balancingOrder.getPrice());
		TestUtils.assertEquals("0.2", balancingOrder.getAmount());
		assertEquals(MockOrderService.BIT, balancingOrder.getMarket());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		assertEquals(Side.BID, balancingOrder.getSide());
		awaitNoMoreOrders(3, 100);
	}

	@Test
	public void testMisMatchedFillsAskFillsLargerAskSideClosedNothingThere() throws InterruptedException, NotEnoughFundsException {
		AtomicReference<Tuple2<Order, Order>> openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5", openingOrders);
		pollForStatuses(openingOrders.get().getA().getClientOrderId(), OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		pollForStatuses(openingOrders.get().getB().getClientOrderId(), OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13510"}, new String[]{"3"}, new String[]{"0.13610"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13510"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.3"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED});
		pollingService.poll();
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(openingOrders.get().getA().getClientOrderId(), 100);
		shortOrder = orderService.pollOrderUpdate(openingOrders.get().getB().getClientOrderId(), 100);
		assertNotNull(longOrder);
		assertNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		awaitNoMoreOrders(2, 100);
	}
	
	private void pollForStatuses(String clientOrderId, OrderStatus... statuses) throws InterruptedException {
		for (OrderStatus status : statuses) {
			Order order = orderService.pollOrderUpdate(clientOrderId, 100);
			assertNotNull("Was execting an order with status " + status, order);
			assertEquals(status, order.getOrderStatus());
		}
	}

	private void awaitNumOrders(int numOrders, long timeout) throws InterruptedException {
		long now = System.currentTimeMillis();
		while (orderService.getNumPlacedOrders() != numOrders && System.currentTimeMillis() < (now + timeout)) {
			Thread.sleep(1);
		}
		if (orderService.getNumPlacedOrders() != numOrders) 
			fail("Expected " + numOrders + " but got " + orderService.getNumPlacedOrders());
	}

	private void awaitNoMoreOrders(int numOrders, long timeout) throws InterruptedException {
		long now = System.currentTimeMillis();
		while (System.currentTimeMillis() < (now + timeout)) {
			Thread.sleep((now + timeout) - System.currentTimeMillis());
		}
		if (orderService.getNumPlacedOrders() != numOrders) 
			fail("Expected " + numOrders + " but got " + orderService.getNumPlacedOrders());
	}

	private ExecutionMonitor createExecutionMonitor(String longPrice, String shortPrice,
			String longAmount, String shortAmount, AtomicReference<Tuple2<Order, Order>> openingOrders) throws NotEnoughFundsException, InterruptedException {
		return createExecutionMonitor("ETHBTC", longPrice, shortPrice, longAmount, shortAmount, MockOrderService.BIT,
				MockOrderService.POL, OrderStatus.OPEN, OrderStatus.OPEN, openingOrders);
	}

	private ExecutionMonitor createExecutionMonitor(String currencyPair, String longPrice, String shortPrice,
			String longAmount, String shortAmount, String longMarket, String shortMarket, 
			OrderStatus longOpenStatus, OrderStatus shortOpenStatus, AtomicReference<Tuple2<Order, Order>> openingOrders) throws NotEnoughFundsException, InterruptedException {
		CurrencyPair pair = TestUtils.cpFor(currencyPair);
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{longOpenStatus, shortOpenStatus});
		ArbInstruction arbInstruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1"), 
				pair.getQuotedCurrency(), new Tuple2<CurrencyPair, CurrencyPair>(pair, pair), new BidAskPrice(new BigDecimal(longPrice), new BigDecimal(shortPrice)), 
				new BidAskAmount(new TradeAmount(new BigDecimal(longAmount), BigDecimal.ZERO), new TradeAmount(new BigDecimal(shortAmount), BigDecimal.ZERO)), 
						new BidAskMarket(longMarket, shortMarket), null, true);
		if (openingOrders == null)
			openingOrders = new AtomicReference<Tuple2<Order, Order>>();
		ExecutionMonitor executionMonitor =  new ExecutionMonitor(orderService, balanceService, arbService, 
				backOfficeService, arbInstruction, pollingService, 0);
		assertEquals("Expected execution monitor to create two orders", 2, orderService.getNumPlacedOrders());
		openingOrders.set(orderService.getLastOrders());
		return executionMonitor;
	}
	
	private class TestPollingService implements PollingService {

		private Map<String, PollListener> listeners = new HashMap<String, PollListener>();

		private void poll() {
			listeners.values().forEach(listener -> {
				listener.polled();
			});
		}
		
		@Override
		public void registerListener(String name, PollListener listener) {
			listeners.put(name, listener);
		}

		@Override
		public void unRegisterListener(String name) {
			listeners.remove(name);
		}

		@Override
		public void changePollingInterval(long interval) {
		}

		@Override
		public void restorePollingInterval() {
		}
		
	}
	
	private class TestArbService implements ArbService {

		final ArbInspector arbInspector;
		boolean suspended;
		Map<String, OrderBook> testBooks = new HashMap<String, OrderBook>();
		
		TestArbService(ArbInspector arbInspector) {
			this.arbInspector = arbInspector;
		}
		
		@Override
		public void suspend(boolean suspend) {
			this.suspended = suspend;
		}

		@Override
		public boolean isSuspended() {
			return suspended;
		}

		@Override
		public OrderBook getOrderBook(String market, String currencyPairName) {
			return testBooks.get(market + ":" + currencyPairName);
		}

		@Override
		public ArbInstruction calculateArb(OrderBook market1, OrderBook market2, BigDecimal amountMarket1,
				BigDecimal amountMarket2) {
			return arbInspector.resolve(market1, market2, amountMarket1, amountMarket2);
		}
		
	}
	
}
