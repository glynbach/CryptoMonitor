package com.kieral.cryptomon.service.arb;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.BalanceHandler;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class TestBasicArbExaminer {

	BasicArbExaminer arbExaminer;
	OrderBookConfig orderBookConfig;
	BalanceHandler balanceHandler;
	OrderBookManager orderBookManager;
	
	@Before
	public void setUp() {
		arbExaminer = new BasicArbExaminer();
		orderBookConfig = TestUtils.obConfig();
		balanceHandler = new BalanceHandler();
		orderBookManager = new OrderBookManager();
		ReflectionTestUtils.setField(arbExaminer, "balanceHandler", balanceHandler);
		ReflectionTestUtils.setField(arbExaminer, "orderBookManager", orderBookManager);
		ReflectionTestUtils.setField(arbExaminer, "orderBookConfig", orderBookConfig);
		ReflectionTestUtils.setField(orderBookManager, "orderBookConfig", orderBookConfig);
		balanceHandler.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test1", Currency.LTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test1", Currency.ETH, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test2", Currency.BTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test2", Currency.ETH, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test3", Currency.BTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test3", Currency.LTC, new BigDecimal(100), true);
		balanceHandler.setConfirmedBalance("Test3", Currency.ETH, new BigDecimal(100), true);
	}
	
	@Test
	public void testNothingThereNormalMarkets() {
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950200"}, new String[]{"10000"}, new String[]{"0.01960200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testSomethingThereCrossedMarkets() {
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals(2, instruction.getLegs().size());
		assertEquals("Test1", instruction.getLegs().get(0).getMarket());
		assertEquals("0.01960100", instruction.getLegs().get(0).getPrice().toPlainString());
		assertEquals("100", instruction.getLegs().get(0).getAmount().toPlainString());
		assertEquals(Side.BID, instruction.getLegs().get(0).getSide());
		assertEquals("Test2", instruction.getLegs().get(1).getMarket());
		assertEquals("0.01970200", instruction.getLegs().get(1).getPrice().toPlainString());
		assertEquals("100", instruction.getLegs().get(1).getAmount().toPlainString());
		assertEquals(Side.ASK, instruction.getLegs().get(1).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.01007475")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestAmount() {
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"50"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals(2, instruction.getLegs().size());
		assertEquals("Test1", instruction.getLegs().get(0).getMarket());
		assertEquals("0.01960100", instruction.getLegs().get(0).getPrice().toPlainString());
		assertEquals("50", instruction.getLegs().get(0).getAmount().toPlainString());
		assertEquals(Side.BID, instruction.getLegs().get(0).getSide());
		assertEquals("Test2", instruction.getLegs().get(1).getMarket());
		assertEquals("0.01970200", instruction.getLegs().get(1).getPrice().toPlainString());
		assertEquals("50", instruction.getLegs().get(1).getAmount().toPlainString());
		assertEquals(Side.ASK, instruction.getLegs().get(1).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00503737")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestSellBalance() {
		balanceHandler.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(20), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals(2, instruction.getLegs().size());
		assertEquals("Test1", instruction.getLegs().get(0).getMarket());
		assertEquals("0.01960100", instruction.getLegs().get(0).getPrice().toPlainString());
		assertEquals("20", instruction.getLegs().get(0).getAmount().toPlainString());
		assertEquals(Side.BID, instruction.getLegs().get(0).getSide());
		assertEquals("Test2", instruction.getLegs().get(1).getMarket());
		assertEquals("0.01970200", instruction.getLegs().get(1).getPrice().toPlainString());
		assertEquals("20", instruction.getLegs().get(1).getAmount().toPlainString());
		assertEquals(Side.ASK, instruction.getLegs().get(1).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00201495")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestBuyBalance() {
		balanceHandler.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(1), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals(2, instruction.getLegs().size());
		assertEquals("Test1", instruction.getLegs().get(0).getMarket());
		assertEquals("0.01960100", instruction.getLegs().get(0).getPrice().toPlainString());
		assertEquals("51.01780521", instruction.getLegs().get(0).getAmount().toPlainString());
		assertEquals(Side.BID, instruction.getLegs().get(0).getSide());
		assertEquals("Test2", instruction.getLegs().get(1).getMarket());
		assertEquals("0.01970200", instruction.getLegs().get(1).getPrice().toPlainString());
		assertEquals("51.01780521", instruction.getLegs().get(1).getAmount().toPlainString());
		assertEquals(Side.ASK, instruction.getLegs().get(1).getSide());
		assertEquals("Unexpected estimation " + instruction.getEstimatedValue(), 0
				, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00513992")));
	}
	
	@Test
	public void testNothingThereCrossedMarketsNoBalanceSellSide() {
		balanceHandler.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(0), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsNoBalanceBuySide() {
		balanceHandler.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(0), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsInsignificantBalanceSellSide() {
		balanceHandler.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(0.001), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsInsignificantBalanceBuySide() {
		balanceHandler.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(0.0001), true);
		ArbInstruction instruction = arbExaminer.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	private CurrencyPair cp(Currency currency1, Currency currency2) {
		return TestUtils.cp(currency1, currency2, new BigDecimal("0.25"));
	}
}
