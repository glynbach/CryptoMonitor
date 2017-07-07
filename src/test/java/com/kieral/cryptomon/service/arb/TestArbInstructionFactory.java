package com.kieral.cryptomon.service.arb;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.util.Tuple2;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class TestArbInstructionFactory {

	@Test
	public void testCreatesArbInstruction() {
		ArbInstruction instruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1234"), Currency.BTC, 
				new Tuple2<CurrencyPair, CurrencyPair>(TestUtils.cpFor("ETHBTC"), TestUtils.cpFor("ETHBTC")), 
				new BidAskPrice(new BigDecimal("1.544"), new BigDecimal("1.545")), 
				new BidAskAmount(new TradeAmount(new BigDecimal("1000"), new BigDecimal("1544")), new TradeAmount(new BigDecimal("1000"), new BigDecimal("1545"))), 
				new BidAskMarket("poloniex", "bittrex"), 
				new Tuple2<OrderBook, OrderBook>(TestUtils.ob("poloniex", TestUtils.cpFor("ETHBTC")), TestUtils.ob("bittrex", TestUtils.cpFor("ETHBTC"))), false);
		assertEquals(ArbDecision.HIGH, instruction.getDecision());
		assertEquals(new BigDecimal("0.1234"), instruction.getEstimatedValue());
		assertEquals("ETHBTC", instruction.getLeg(Side.BID).getCurrencyPair().getName());
		assertEquals("poloniex", instruction.getLeg(Side.BID).getMarket());
		assertEquals(new BigDecimal("1.544"), instruction.getLeg(Side.BID).getPrice());
		assertEquals(new BigDecimal("1000"), instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals("ETHBTC", instruction.getLeg(Side.ASK).getCurrencyPair().getName());
		assertEquals("bittrex", instruction.getLeg(Side.ASK).getMarket());
		assertEquals(new BigDecimal("1.545"), instruction.getLeg(Side.ASK).getPrice());
		assertEquals(new BigDecimal("1000"), instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
	}
	
	@Test
	public void testCreatesAdjustedArbInstructionAmount() {
		ArbInstruction instruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1234"), Currency.BTC, 
				new Tuple2<CurrencyPair, CurrencyPair>(TestUtils.cpFor("ETHBTC"), TestUtils.cpFor("ETHBTC")), 
				new BidAskPrice(new BigDecimal("1.544"), new BigDecimal("1.545")), 
				new BidAskAmount(new TradeAmount(new BigDecimal("1000"), new BigDecimal("1544")), new TradeAmount(new BigDecimal("1000"), new BigDecimal("1545"))), 
				new BidAskMarket("poloniex", "bittrex"), 
				new Tuple2<OrderBook, OrderBook>(TestUtils.ob("poloniex", TestUtils.cpFor("ETHBTC")), TestUtils.ob("bittrex", TestUtils.cpFor("ETHBTC"))), false);
		instruction = ArbInstructionFactory.createArbInstruction(instruction, new BigDecimal("500"));
		assertEquals(ArbDecision.HIGH, instruction.getDecision());
		assertEquals(BigDecimal.ZERO, instruction.getEstimatedValue());
		assertEquals("ETHBTC", instruction.getLeg(Side.BID).getCurrencyPair().getName());
		assertEquals("poloniex", instruction.getLeg(Side.BID).getMarket());
		assertEquals(new BigDecimal("1.544"), instruction.getLeg(Side.BID).getPrice());
		assertEquals(new BigDecimal("500"), instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals("ETHBTC", instruction.getLeg(Side.ASK).getCurrencyPair().getName());
		assertEquals("bittrex", instruction.getLeg(Side.ASK).getMarket());
		assertEquals(new BigDecimal("1.545"), instruction.getLeg(Side.ASK).getPrice());
		assertEquals(new BigDecimal("500"), instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
	}

	@Test
	public void testCreatesAdjustedOneSidedArbInstructionLongPosition() {
		ArbInstruction instruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1234"), Currency.BTC, 
				new Tuple2<CurrencyPair, CurrencyPair>(TestUtils.cpFor("ETHBTC"), TestUtils.cpFor("ETHBTC")), 
				new BidAskPrice(new BigDecimal("1.544"), new BigDecimal("1.545")), 
				new BidAskAmount(new TradeAmount(new BigDecimal("1000"), new BigDecimal("1544")), new TradeAmount(new BigDecimal("1000"), new BigDecimal("1545"))), 
				new BidAskMarket("poloniex", "bittrex"), 
				new Tuple2<OrderBook, OrderBook>(TestUtils.ob("poloniex", TestUtils.cpFor("ETHBTC")), TestUtils.ob("bittrex", TestUtils.cpFor("ETHBTC"))), false);
		instruction = ArbInstructionFactory.createArbInstruction(instruction, Side.BID, new Tuple2<BigDecimal, BigDecimal>(new BigDecimal("1.545"), new BigDecimal("500")));
		assertEquals(ArbDecision.HIGH, instruction.getDecision());
		assertEquals(BigDecimal.ZERO, instruction.getEstimatedValue());
		assertEquals("ETHBTC", instruction.getLeg(Side.BID).getCurrencyPair().getName());
		assertEquals("poloniex", instruction.getLeg(Side.BID).getMarket());
		assertEquals(new BigDecimal("1.545"), instruction.getLeg(Side.BID).getPrice());
		assertEquals(new BigDecimal("500"), instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals("ETHBTC", instruction.getLeg(Side.ASK).getCurrencyPair().getName());
		assertEquals("bittrex", instruction.getLeg(Side.ASK).getMarket());
		assertEquals(BigDecimal.ZERO, instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
	}

	@Test
	public void testCreatesAdjustedOneSidedArbInstructionShortPosition() {
		ArbInstruction instruction = ArbInstructionFactory.createArbInstruction(ArbDecision.HIGH, new BigDecimal("0.1234"), Currency.BTC, 
				new Tuple2<CurrencyPair, CurrencyPair>(TestUtils.cpFor("ETHBTC"), TestUtils.cpFor("ETHBTC")), 
				new BidAskPrice(new BigDecimal("1.544"), new BigDecimal("1.545")), 
				new BidAskAmount(new TradeAmount(new BigDecimal("1000"), new BigDecimal("1544")), new TradeAmount(new BigDecimal("1000"), new BigDecimal("1545"))), 
				new BidAskMarket("poloniex", "bittrex"), 
				new Tuple2<OrderBook, OrderBook>(TestUtils.ob("poloniex", TestUtils.cpFor("ETHBTC")), TestUtils.ob("bittrex", TestUtils.cpFor("ETHBTC"))), false);
		instruction = ArbInstructionFactory.createArbInstruction(instruction, Side.ASK, new Tuple2<BigDecimal, BigDecimal>(new BigDecimal("1.544"), new BigDecimal("500")));
		assertEquals(ArbDecision.HIGH, instruction.getDecision());
		assertEquals(BigDecimal.ZERO, instruction.getEstimatedValue());
		assertEquals("ETHBTC", instruction.getLeg(Side.BID).getCurrencyPair().getName());
		assertEquals("poloniex", instruction.getLeg(Side.BID).getMarket());
		assertEquals(BigDecimal.ZERO, instruction.getLeg(Side.BID).getAmount().getBaseAmount());
		assertEquals("ETHBTC", instruction.getLeg(Side.ASK).getCurrencyPair().getName());
		assertEquals("bittrex", instruction.getLeg(Side.ASK).getMarket());
		assertEquals(new BigDecimal("1.544"), instruction.getLeg(Side.ASK).getPrice());
		assertEquals(new BigDecimal("500"), instruction.getLeg(Side.ASK).getAmount().getBaseAmount());
	}

}
