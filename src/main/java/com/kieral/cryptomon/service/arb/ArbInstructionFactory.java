package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.sided.SidedValue;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;
import com.kieral.cryptomon.service.arb.ArbInstruction.SidedArbInstructionLeg;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.service.util.Tuple2;

public class ArbInstructionFactory {

	private final static AtomicReference<ArbInstructionFactory> instance = new AtomicReference<ArbInstructionFactory>();
	
	public static ArbInstruction createNoArbInstruction(String reason) {
		return getInstance().doCreateNoArbInstruction(reason);
	}
	
	public static ArbInstruction createArbInstruction(ArbDecision decision, BigDecimal estimatedValue, Currency valueCurrency,
			Tuple2<CurrencyPair, CurrencyPair> currencyPairs, BidAskPrice prices, BidAskAmount amounts, BidAskMarket markets, 
			Tuple2<OrderBook,OrderBook> referenceBooks, boolean resolvingOpenArb) {
		return getInstance().doCreateArbInstruction(decision, estimatedValue, valueCurrency, currencyPairs, 
				prices, amounts, markets, referenceBooks, resolvingOpenArb);
	}

	/**
	 * Used for low value rebalancing arbs
	 */
	public static ArbInstruction createArbInstruction(ArbInstruction instruction, BigDecimal desiredNewBaseAmount) {
		return getInstance().doCreateArbInstruction(instruction, desiredNewBaseAmount);
	}

	private static ArbInstructionFactory getInstance() {
		if (instance.get() == null)
			instance.compareAndSet(null, new ArbInstructionFactory());
		return instance.get();
	}
	
	private ArbInstruction doCreateNoArbInstruction(String reason) {
		return new ArbInstruction(ArbDecision.NOTHING_THERE, BigDecimal.ZERO, null, reason, null); 
	}

	private ArbInstruction doCreateArbInstruction(ArbInstruction instruction, BigDecimal desiredNewBaseAmount) {
		if (instruction == null)
			throw new IllegalStateException("instruction can not be null");
		if (desiredNewBaseAmount == null || CommonUtils.isZero(desiredNewBaseAmount)) {
			throw new IllegalStateException("invalid desiredNewBaseAmount " + desiredNewBaseAmount);
		}
		ArbInstructionLeg longLeg = instruction.getLeg(Side.BID);
		TradeAmount longAmount = longLeg.getAmount();
		if (longAmount.getBaseAmount().compareTo(desiredNewBaseAmount) <= 0)
			return instruction;
		ArbInstructionLeg shortLeg = instruction.getLeg(Side.ASK);
		TradeAmount shortAmount = shortLeg.getAmount();
		if (shortAmount.getBaseAmount().compareTo(desiredNewBaseAmount) <= 0)
			return instruction;
		BigDecimal price = longLeg.getPrice();
		TradeAmount newLongTradeAmount = new TradeAmount(desiredNewBaseAmount, desiredNewBaseAmount.multiply(price).setScale(8, RoundingMode.HALF_DOWN)); 
		price = shortLeg.getPrice();
		TradeAmount newShortTradeAmount = new TradeAmount(desiredNewBaseAmount, desiredNewBaseAmount.multiply(price).setScale(8, RoundingMode.HALF_DOWN)); 
		SidedArbInstructionLeg legs = new SidedArbInstructionLeg(
				new ArbInstructionLeg(longLeg.getMarket(), Side.BID, longLeg.getCurrencyPair(), longLeg.getPrice(), newLongTradeAmount),
				new ArbInstructionLeg(shortLeg.getMarket(), Side.ASK, shortLeg.getCurrencyPair(), shortLeg.getPrice(), newShortTradeAmount));
		return new ArbInstruction(instruction.getDecision(), BigDecimal.ZERO, legs, null, instruction.getOrderBooks());
	}
	

	private ArbInstruction doCreateArbInstruction(ArbDecision decision, BigDecimal estimatedValue, Currency valueCurrency,
			Tuple2<CurrencyPair, CurrencyPair> currencyPairs, BidAskPrice prices, BidAskAmount amounts, BidAskMarket markets, 
			Tuple2<OrderBook, OrderBook> referenceBooks, boolean resolvingOpenArb) {
		if (!resolvingOpenArb) {
			if (decision == null || estimatedValue == null || valueCurrency == null || currencyPairs.getA() == null || currencyPairs.getB() == null || 
					!validatePositive(prices) || !validatePositive(amounts) || !validate(markets))
				throw new IllegalStateException(String.format("Invalid conditions for an arb instruction %s %s %s %s %s %s",
						decision, estimatedValue, valueCurrency, prices, amounts, markets));
		}
		SidedArbInstructionLeg legs = new SidedArbInstructionLeg(
				new ArbInstructionLeg(markets.get(Side.BID), Side.BID, currencyPairs.getA(), prices.get(Side.BID), amounts.get(Side.BID)),
				new ArbInstructionLeg(markets.get(Side.ASK), Side.ASK, currencyPairs.getB(), prices.get(Side.ASK), amounts.get(Side.ASK)));
		return new ArbInstruction(decision, estimatedValue, legs, null, referenceBooks);
	}
	
	private boolean validate(SidedValue<?> values) {
		if (values == null || values.get(Side.BID) == null || values.get(Side.ASK) == null)
			return false;
		return true;
	}
	
	private boolean validatePositive(SidedValue<BigDecimal> values) {
		if (validate(values) &&
				BigDecimal.ZERO.compareTo(values.get(Side.BID)) < 0 &&
				BigDecimal.ZERO.compareTo(values.get(Side.ASK)) < 0)
			return true;
		return false;
	}

	private boolean validatePositive(BidAskAmount values) {
		if (validate(values) && 
				BigDecimal.ZERO.compareTo(values.get(Side.BID).getBaseAmount()) < 0 &&
				BigDecimal.ZERO.compareTo(values.get(Side.ASK).getBaseAmount()) < 0)
			return true;
		return false;
	}

}
