package com.kieral.cryptomon.service.arb;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class TestSimpleArbInspector {

	SimpleArbInspector arbInspector;
	OrderBookConfig orderBookConfig;
	BalanceService balanceService;
	OrderBookManager orderBookManager;
	
	@Before
	public void setUp() {
		arbInspector = new SimpleArbInspector();
		orderBookConfig = TestUtils.obConfig();
		balanceService = new BalanceService();
		orderBookManager = new OrderBookManager();
		ReflectionTestUtils.setField(arbInspector, "balanceService", balanceService);
		ReflectionTestUtils.setField(arbInspector, "orderBookManager", orderBookManager);
		ReflectionTestUtils.setField(arbInspector, "orderBookConfig", orderBookConfig);
		ReflectionTestUtils.setField(orderBookManager, "orderBookConfig", orderBookConfig);
		balanceService.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test1", Currency.LTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test1", Currency.ETH, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test2", Currency.BTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test2", Currency.ETH, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test3", Currency.BTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test3", Currency.LTC, new BigDecimal(100), true);
		balanceService.setConfirmedBalance("Test3", Currency.ETH, new BigDecimal(100), true);
	}
	
	@Test
	public void testNothingThereNormalMarkets() {
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950200"}, new String[]{"10000"}, new String[]{"0.01960200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testSomethingThereCrossedMarkets() {
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals("Test1", instruction.getLeg(Side.BID).getMarket());
		assertEquals("0.01960100", instruction.getLeg(Side.BID).getPrice().toPlainString());
		// we're only taking 99% of the opportunity
		TestUtils.assertEquals("99", instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals(Side.BID, instruction.getLeg(Side.BID).getSide());
		assertEquals("Test2", instruction.getLeg(Side.ASK).getMarket());
		assertEquals("0.01970200", instruction.getLeg(Side.ASK).getPrice().toPlainString());
		TestUtils.assertEquals("99", instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
		assertEquals(Side.ASK, instruction.getLeg(Side.ASK).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00027150")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestAmount() {
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"50"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals("Test1", instruction.getLeg(Side.BID).getMarket());
		assertEquals("0.01960100", instruction.getLeg(Side.BID).getPrice().toPlainString());
		// we're only taking 99% of the opportunity
		TestUtils.assertEquals("49.5", instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals(Side.BID, instruction.getLeg(Side.BID).getSide());
		assertEquals("Test2", instruction.getLeg(Side.ASK).getMarket());
		assertEquals("0.01970200", instruction.getLeg(Side.ASK).getPrice().toPlainString());
		TestUtils.assertEquals("49.5", instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
		assertEquals(Side.ASK, instruction.getLeg(Side.ASK).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00013576")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestSellBalance() {
		balanceService.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(20), true);
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals("Test1", instruction.getLeg(Side.BID).getMarket());
		assertEquals("0.01960100", instruction.getLeg(Side.BID).getPrice().toPlainString());
		// we're only taking 99% of the opportunity
		TestUtils.assertEquals("19.8", instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals(Side.BID, instruction.getLeg(Side.BID).getSide());
		assertEquals("Test2", instruction.getLeg(Side.ASK).getMarket());
		assertEquals("0.01970200", instruction.getLeg(Side.ASK).getPrice().toPlainString());
		TestUtils.assertEquals("19.8", instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
		assertEquals(Side.ASK, instruction.getLeg(Side.ASK).getSide());
		assertEquals(0, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00005430")));
	}

	@Test
	public void testSomethingThereCrossedMarketsUsesLowestBuyBalance() {
		balanceService.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(1), true);
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertTrue(ArbInstruction.SOMETHING_THERE.contains(instruction.getDecision()));
		assertNotNull(instruction.getLegs());
		assertEquals("Test1", instruction.getLeg(Side.BID).getMarket());
		assertEquals("0.01960100", instruction.getLeg(Side.BID).getPrice().toPlainString());
		// we're only taking 99% of the opportunity
		assertEquals("50.50762716", instruction.getLeg(Side.BID).getAmount().getBaseAmount().toPlainString());
		assertEquals(Side.BID, instruction.getLeg(Side.BID).getSide());
		assertEquals("Test2", instruction.getLeg(Side.ASK).getMarket());
		assertEquals("0.01970200", instruction.getLeg(Side.ASK).getPrice().toPlainString());
		assertEquals("50.50762716", instruction.getLeg(Side.ASK).getAmount().getBaseAmount().toPlainString());
		assertEquals(Side.ASK, instruction.getLeg(Side.ASK).getSide());
		assertEquals("Unexpected estimation " + instruction.getEstimatedValue(), 0
				, instruction.getEstimatedValue().compareTo(new BigDecimal("0.00013852")));
	}
	
	@Test
	public void testNothingThereCrossedMarketsNoBalanceSellSide() {
		balanceService.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(0), true);
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsNoBalanceBuySide() {
		balanceService.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(0), true);
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsInsignificantBalanceSellSide() {
		balanceService.setConfirmedBalance("Test2", Currency.LTC, new BigDecimal(0.001), true);
		ArbInstruction instruction = arbInspector.examine(
				TestUtils.ob("Test1", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01950100"}, new String[]{"10000"}, new String[]{"0.01960100"}, new String[]{"10000"}), 
				TestUtils.ob("Test2", cp(Currency.LTC, Currency.BTC), 
						new String[]{"0.01970200"}, new String[]{"10000"}, new String[]{"0.01980200"}, new String[]{"10000"}));
		assertEquals(ArbDecision.NOTHING_THERE, instruction.getDecision());
	}

	@Test
	public void testNothingThereCrossedMarketsInsignificantBalanceBuySide() {
		balanceService.setConfirmedBalance("Test1", Currency.BTC, new BigDecimal(0.0001), true);
		ArbInstruction instruction = arbInspector.examine(
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
