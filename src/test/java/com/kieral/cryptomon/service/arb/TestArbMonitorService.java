package com.kieral.cryptomon.service.arb;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.service.BalanceHandler;
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.exchange.IExchangeService;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.liquidity.IOrderBookListener;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class TestArbMonitorService {

	final static BigDecimal LOW_MAGIC_NUMBER = new BigDecimal("9");
	final static BigDecimal HIGH_MAGIC_NUMBER = new BigDecimal("99");
	final static BigDecimal CANCEL_MAGIC_NUMBER = new BigDecimal("999");
	
	final static BidAskPrice prices = new BidAskPrice(BigDecimal.ONE, BigDecimal.ONE);
	final static BidAskAmount amounts = new BidAskAmount(BigDecimal.ONE, BigDecimal.ONE);
	
	ExchangeManagerService exchangeManager;
	BalanceHandler balanceHandler;
	IArbExaminer arbExaminer;
	IArbInstructionHandler instructionHandler;
	
	IOrderBookListener obListener1;
	IOrderBookListener obListener2;
	IOrderBookListener obListener3;
	IStatusListener statusListener1;
	IStatusListener statusListener2;
	IStatusListener statusListener3;
	List<ArbInstruction> arbInstructions;
	
	ArbMonitorService arbMonitorService;

	@Test
	public void testCanCompareTwoOrderBooks() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		assertEquals(0, arbInstructions.size());
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
	}

	@Test
	public void testCanCompareThreeOrderBooks() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		obListener3.onOrderBookUpdate(TestUtils.ob("Test3", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		assertEquals(0, arbInstructions.size());
		// 2 more
		Mockito.verify(arbExaminer, Mockito.times(3)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
	}

	@Test
	public void testAgainOnOrderBookUpdate() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		obListener3.onOrderBookUpdate(TestUtils.ob("Test3", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		assertEquals(0, arbInstructions.size());
		// 2 more
		Mockito.verify(arbExaminer, Mockito.times(3)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		obListener3.onOrderBookUpdate(TestUtils.ob("Test3", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		// 2 more
		Mockito.verify(arbExaminer, Mockito.times(5)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
	}

	@Test
	public void testInstructionEmitted() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{LOW_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(1, arbInstructions.size());
		assertEquals(ArbDecision.LOW, arbInstructions.get(0).getDecision());
	}

	@Test
	public void testHighInstructionEmittedAsPriority() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{LOW_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(1, arbInstructions.size());
		assertEquals(ArbDecision.LOW, arbInstructions.get(0).getDecision());
		obListener3.onOrderBookUpdate(TestUtils.ob("Test3", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{HIGH_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		Mockito.verify(arbExaminer, Mockito.times(3)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(2, arbInstructions.size());
		assertEquals(ArbDecision.HIGH, arbInstructions.get(1).getDecision());
	}

	@Test
	public void testCancelInstructionEmittedAsPriority() {
		obListener1.onOrderBookUpdate(TestUtils.ob("Test1", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{LOW_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO)));
		Mockito.verify(arbExaminer, Mockito.times(1)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(1, arbInstructions.size());
		assertEquals(ArbDecision.LOW, arbInstructions.get(0).getDecision());
		obListener3.onOrderBookUpdate(TestUtils.ob("Test3", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{HIGH_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		Mockito.verify(arbExaminer, Mockito.times(3)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(2, arbInstructions.size());
		assertEquals(ArbDecision.HIGH, arbInstructions.get(1).getDecision());
		obListener2.onOrderBookUpdate(TestUtils.ob("Test2", TestUtils.cp(Currency.LTC, Currency.BTC, BigDecimal.ZERO),
				new String[]{CANCEL_MAGIC_NUMBER.toPlainString()}, new String[]{"0.1"}));
		Mockito.verify(arbExaminer, Mockito.times(5)).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		assertEquals(3, arbInstructions.size());
		assertEquals(ArbDecision.CANCEL, arbInstructions.get(2).getDecision());
	}

	@Before
	public void setUp() {
		balanceHandler = new BalanceHandler();
		exchangeManager = Mockito.mock(ExchangeManagerService.class);
		IExchangeService exchange1 = Mockito.mock(IExchangeService.class);
		IExchangeService exchange2 = Mockito.mock(IExchangeService.class);
		IExchangeService exchange3 = Mockito.mock(IExchangeService.class);
		Mockito.when(exchangeManager.getEnabledExchanges()).thenReturn(
				Arrays.asList(new IExchangeService[]{exchange1, exchange2, exchange3}));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				obListener1 = (IOrderBookListener)invocation.getArgument(0);
				return null;
			}}).when(exchange1).registerOrderBookListener(Mockito.any(IOrderBookListener.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				obListener2 = (IOrderBookListener)invocation.getArgument(0);
				return null;
			}}).when(exchange2).registerOrderBookListener(Mockito.any(IOrderBookListener.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				obListener3 = (IOrderBookListener)invocation.getArgument(0);
				return null;
			}}).when(exchange3).registerOrderBookListener(Mockito.any(IOrderBookListener.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				statusListener1 = (IStatusListener)invocation.getArgument(0);
				return null;
			}}).when(exchange1).registerStatusListener(Mockito.any(IStatusListener.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				statusListener2 = (IStatusListener)invocation.getArgument(0);
				return null;
			}}).when(exchange2).registerStatusListener(Mockito.any(IStatusListener.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				statusListener3 = (IStatusListener)invocation.getArgument(0);
				return null;
			}}).when(exchange3).registerStatusListener(Mockito.any(IStatusListener.class));
		arbExaminer = Mockito.mock(IArbExaminer.class);
		Mockito.doAnswer(new Answer<ArbInstruction>() {
			@Override
			public ArbInstruction answer(InvocationOnMock invocation) throws Throwable {
				OrderBook orderBook1 = (OrderBook)invocation.getArgument(0);
				OrderBook orderBook2 = (OrderBook)invocation.getArgument(1);
				BidAskMarket markets = new BidAskMarket(orderBook1.getMarket(), orderBook2.getMarket());
				if ((orderBook1.getBids() != null && orderBook1.getBids().size() > 0)) {
					BigDecimal magicNumber = orderBook1.getBids().get(0).getPrice();
					if ((orderBook2.getBids() != null && orderBook2.getBids().size() > 0)) {
						if (magicNumber.compareTo(orderBook2.getBids().get(0).getPrice()) < 0) {
							magicNumber = orderBook2.getBids().get(0).getPrice();
						}
					}
					if (CANCEL_MAGIC_NUMBER.compareTo(magicNumber) == 0) {
						return ArbInstructionFactory.createArbInstruction(ArbDecision.CANCEL, BigDecimal.ZERO, 
								Currency.BTC, orderBook1.getCurrencyPair(), prices, amounts, markets);
					}
					if (HIGH_MAGIC_NUMBER.compareTo(magicNumber) == 0) {
						return ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, BigDecimal.ZERO, 
								Currency.BTC, orderBook1.getCurrencyPair(), prices, amounts, markets);
					}
					if (LOW_MAGIC_NUMBER.compareTo(magicNumber) == 0) {
						return ArbInstructionFactory.createArbInstruction(ArbDecision.LOW, BigDecimal.ZERO, 
								Currency.BTC, orderBook1.getCurrencyPair(), prices, amounts, markets);
					}
				}
				return ArbInstructionFactory.createNoArbInstruction("");
			}}).when(arbExaminer).examine(Mockito.any(OrderBook.class), Mockito.any(OrderBook.class));
		arbInstructions = new ArrayList<ArbInstruction>();
		instructionHandler = Mockito.mock(IArbInstructionHandler.class);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				arbInstructions.add(invocation.getArgument(0));
				return null;
			}}).when(instructionHandler).onArbInstruction(Mockito.any(ArbInstruction.class));
		arbMonitorService = new ArbMonitorService();
		ReflectionTestUtils.setField(arbMonitorService, "exchangeManager", exchangeManager);
		ReflectionTestUtils.setField(arbMonitorService, "balanceHandler", balanceHandler);
		ReflectionTestUtils.setField(arbMonitorService, "arbExaminer", arbExaminer);
		ReflectionTestUtils.setField(arbMonitorService, "arbInstructionHandler", instructionHandler);
		arbMonitorService.init();
	}
}