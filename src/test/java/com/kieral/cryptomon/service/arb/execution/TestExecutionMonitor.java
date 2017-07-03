package com.kieral.cryptomon.service.arb.execution;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.CurrencyPair;
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
import com.kieral.cryptomon.service.arb.ArbDecision;
import com.kieral.cryptomon.service.arb.ArbInspector;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstructionFactory;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.arb.SimpleArbInspector;
import com.kieral.cryptomon.service.arb.execution.ExecutionMonitor.Sleeper;
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
	AtomicBoolean canProceed = new AtomicBoolean(false);
	CountDownLatch proceedLatch = new CountDownLatch(1);
	CountDownLatch testLatch = new CountDownLatch(1);
	
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
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				testLatch.countDown();
				return null;
			}}).when(backOfficeService).onExecutionCompletion(Mockito.any(ArbInstruction.class), 
				Mockito.any(List.class), Mockito.any(List.class), Mockito.anyBoolean());
		ExecutionMonitor.sleeper = new Sleeper() {
			@Override
			public void sleep(long millis) throws InterruptedException {
				while(!canProceed.compareAndSet(true, false)) {
					if (proceedLatch.getCount() == 0)
						proceedLatch = new CountDownLatch(1);
					proceedLatch.await();
				}
			}};
	}

	@Test
	public void testPlacesOrders() throws InterruptedException, NotEnoughFundsException {
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
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
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.FILLED);
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.FILLED);
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertTrue(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void ResolvesNoFillsOrdersNoChange() throws InterruptedException, NotEnoughFundsException {
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13300"}, new String[]{"3"}, new String[]{"0.13400"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13500"}, new String[]{"1"}, new String[]{"0.13600"}, new String[]{"2"}));
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNull(longOrder);
		assertNull(shortOrder);
	}

	@Test
	public void ResolvesNoFillsOrdersPriceChange() throws InterruptedException, NotEnoughFundsException {
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"3"}, new String[]{"0.13410"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"1"}, new String[]{"0.13590"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
	public void ResolvesNoFillsOrdersPriceChangeUsesLowerBookLevel() throws InterruptedException, NotEnoughFundsException {
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
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
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
	public void ResolvesNothingThereCancelsOut() throws InterruptedException, NotEnoughFundsException {
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13350"}, new String[]{"3"}, new String[]{"0.13450"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"1"}, new String[]{"0.13550"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		Order newLongOrder = orderService.pollOrderUpdate(100);
		Order newShortOrder = orderService.pollOrderUpdate(100);
		assertNull(newLongOrder);
		assertNull(newShortOrder);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void ResolvesPartialFillsOrdersPriceChange() throws InterruptedException, NotEnoughFundsException {
		createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
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
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
	public void ResolvesPartialFillsCancelsNothingThere() throws InterruptedException, NotEnoughFundsException {
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "0.5", "0.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
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
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
						new String[]{"0.13350"}, new String[]{"3"}, new String[]{"0.13450"}, new String[]{"5"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"1"}, new String[]{"0.13550"}, new String[]{"2"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		newLongOrder = orderService.pollOrderUpdate(100);
		newShortOrder = orderService.pollOrderUpdate(100);
		assertNull(newLongOrder);
		assertNull(newShortOrder);
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void ResolvesInbalancedPartialFillsCreatesBalanceTrade() throws InterruptedException, NotEnoughFundsException {
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "2.5", "2.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"13"}, new String[]{"0.13410"}, new String[]{"15"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"11"}, new String[]{"0.13590"}, new String[]{"12"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.4"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("1.1"));
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
						new String[]{"0.13350"}, new String[]{"13"}, new String[]{"0.13450"}, new String[]{"15"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"11"}, new String[]{"0.13550"}, new String[]{"12"}));
		orderService.clearDesiredOrderStatus();
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(100);
		assertNotNull(balancingOrder);
		TestUtils.assertEquals("0.7", balancingOrder.getAmount());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		newLongOrder = orderService.pollOrderUpdate(100);
		newShortOrder = orderService.pollOrderUpdate(100);
		assertNull(newLongOrder);
		assertNull(newShortOrder);
		// let the balancing order be filled
		orderService.clearDesiredOrderStatus();
		orderService.setDefaultDesiredOrderStatus(OrderStatus.FILLED);
		canProceed.set(true);
		proceedLatch.countDown();
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	@Test
	public void ResolvesInbalancedPartialFillsCreatesMultipleBalanceTrades() throws InterruptedException, NotEnoughFundsException {
		ExecutionMonitor executionMonitor = createExecutionMonitor("0.13400", "0.13500", "2.5", "2.5");
		pollForStatuses(OrderStatus.SENDING);
		Order longOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order shortOrder = orderService.pollOrderUpdate(100);
		arbService.testBooks.put(MockOrderService.BIT + ":" + longOrder.getCurrencyPair().getName(), 
				TestUtils.ob(longOrder.getMarket(), longOrder.getCurrencyPair(), 
						new String[]{"0.13310"}, new String[]{"13"}, new String[]{"0.13410"}, new String[]{"15"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13490"}, new String[]{"11"}, new String[]{"0.13590"}, new String[]{"12"}));
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.OPEN});
		orderService.setDesiredOrderStatus(longOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("0.4"));
		orderService.setDesiredOrderStatus(shortOrder.getClientOrderId(), OrderStatus.PARTIALLY_FILLED, new BigDecimal("1.1"));
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.PARTIALLY_FILLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.PARTIALLY_FILLED, shortOrder.getOrderStatus());
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newLongOrder = orderService.pollOrderUpdate(100);
		pollForStatuses(OrderStatus.SENDING);
		Order newShortOrder = orderService.pollOrderUpdate(100);
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
						new String[]{"0.13350"}, new String[]{"13"}, new String[]{"0.13450"}, new String[]{"15"}));
		arbService.testBooks.put(MockOrderService.POL + ":" + shortOrder.getCurrencyPair().getName(), 
				TestUtils.ob(shortOrder.getMarket(), shortOrder.getCurrencyPair(), 
						new String[]{"0.13450"}, new String[]{"11"}, new String[]{"0.13550"}, new String[]{"12"}));
		orderService.clearDesiredOrderStatus();
		orderService.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, OrderStatus.CANCELLED});
		canProceed.set(true);
		proceedLatch.countDown();
		longOrder = orderService.pollOrderUpdate(100);
		shortOrder = orderService.pollOrderUpdate(100);
		assertNotNull(longOrder);
		assertNotNull(shortOrder);
		assertEquals(OrderStatus.CANCELLED, longOrder.getOrderStatus());
		assertEquals(OrderStatus.CANCELLED, shortOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order balancingOrder = orderService.pollOrderUpdate(100);
		assertNotNull(balancingOrder);
		TestUtils.assertEquals("0.7", balancingOrder.getAmount());
		assertEquals(OrderStatus.OPEN, balancingOrder.getOrderStatus());
		newLongOrder = orderService.pollOrderUpdate(100);
		newShortOrder = orderService.pollOrderUpdate(100);
		assertNull(newLongOrder);
		assertNull(newShortOrder);
		// let the balancing order be partially filled
		orderService.clearDesiredOrderStatus();
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{OrderStatus.CANCELLED, 
				OrderStatus.OPEN, OrderStatus.CANCELLED});
		orderService.setDefaultDesiredOrderStatus(OrderStatus.PARTIALLY_FILLED);
		canProceed.set(true);
		proceedLatch.countDown();
		balancingOrder = orderService.pollOrderUpdate(100);
		assertEquals(OrderStatus.PARTIALLY_FILLED, balancingOrder.getOrderStatus());
		balancingOrder = orderService.pollOrderUpdate(100);
		assertEquals(OrderStatus.CANCELLED, balancingOrder.getOrderStatus());
		pollForStatuses(OrderStatus.SENDING);
		Order newBalancingOrder = orderService.pollOrderUpdate(100);
		assertNotNull(newBalancingOrder);
		assertTrue(new BigDecimal("0.7").compareTo(newBalancingOrder.getAmount()) > 0);
		assertEquals(OrderStatus.OPEN, newBalancingOrder.getOrderStatus());
		// The ExecutionMonitor only waits 5 seconds until giving up
		testLatch.await(10000, TimeUnit.MILLISECONDS);
		if (testLatch.getCount() > 0)
			fail("Expected execution monitor to be complete");
		assertFalse(executionMonitor.isDone());
		assertTrue(executionMonitor.isClosed());
	}

	private void pollForStatuses(OrderStatus... statuses) throws InterruptedException {
		for (OrderStatus status : statuses) {
			Order order = orderService.pollOrderUpdate(100);
			assertNotNull("Was execting an order with status " + status, order);
			assertEquals(status, order.getOrderStatus());
		}
	}
	
	private ExecutionMonitor createExecutionMonitor(String longPrice, String shortPrice,
			String longAmount, String shortAmount) throws NotEnoughFundsException, InterruptedException {
		return createExecutionMonitor("ETHBTC", longPrice, shortPrice, longAmount, shortAmount, MockOrderService.BIT,
				MockOrderService.POL, OrderStatus.OPEN, OrderStatus.OPEN);
	}

	private ExecutionMonitor createExecutionMonitor(String currencyPair, String longPrice, String shortPrice,
			String longAmount, String shortAmount, String longMarket, String shortMarket, 
			OrderStatus longOpenStatus, OrderStatus shortOpenStatus) throws NotEnoughFundsException, InterruptedException {
		CurrencyPair pair = TestUtils.cpFor(currencyPair);
		orderService.setDesiredActionOrderStatuses(new OrderStatus[]{longOpenStatus, shortOpenStatus});
		ArbInstruction arbInstruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1"), 
				pair.getQuotedCurrency(), new Tuple2<CurrencyPair, CurrencyPair>(pair, pair), new BidAskPrice(new BigDecimal(longPrice), new BigDecimal(shortPrice)), 
				new BidAskAmount(new TradeAmount(new BigDecimal(longAmount), BigDecimal.ZERO), new TradeAmount(new BigDecimal(shortAmount), BigDecimal.ZERO)), 
						new BidAskMarket(longMarket, shortMarket), null, true);
		ExecutionMonitor executionMonitor =  new ExecutionMonitor(orderService, balanceService, arbService, 
				backOfficeService, arbInstruction, 0);
		canProceed = new AtomicBoolean(false);
		return executionMonitor;
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
