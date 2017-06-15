package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.sided.SidedValue;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;

public class ArbInstructionFactory {

	private final static AtomicReference<ArbInstructionFactory> instance = new AtomicReference<ArbInstructionFactory>();
	
	public static ArbInstruction createNoArbInstruction(String reason) {
		return getInstance().doCreateNoArbInstruction(reason);
	}
	
	public static ArbInstruction createArbInstruction(ArbDecision decision, BigDecimal estimatedValue, Currency valueCurrency,
			CurrencyPair currencyPair, BidAskPrice prices, BidAskAmount amounts, BidAskMarket markets) {
		return getInstance().doCreateArbInstruction(decision, estimatedValue, valueCurrency, currencyPair, prices, amounts, markets);
	}
	
	private static ArbInstructionFactory getInstance() {
		if (instance.get() == null)
			instance.compareAndSet(null, new ArbInstructionFactory());
		return instance.get();
	}
	
	private ArbInstruction doCreateNoArbInstruction(String reason) {
		return new ArbInstruction(ArbDecision.NOTHING_THERE, BigDecimal.ZERO, null, reason); 
	}
	
	private ArbInstruction doCreateArbInstruction(ArbDecision decision, BigDecimal estimatedValue, Currency valueCurrency,
			CurrencyPair currencyPair, BidAskPrice prices, BidAskAmount amounts, BidAskMarket markets) {
		if (decision == null || estimatedValue == null || valueCurrency == null || currencyPair == null || !validatePositive(prices) || 
				!validatePositive(amounts) || !validate(markets))
			throw new IllegalStateException(String.format("Invalid conditions for an arb instruction %s %s %s %s %s %s",
					decision, estimatedValue, valueCurrency, prices, amounts, markets));
		List<ArbInstructionLeg> legs = new ArrayList<ArbInstructionLeg>();
		legs.add(new ArbInstructionLeg(markets.get(Side.BID), Side.BID, currencyPair, prices.get(Side.BID), amounts.get(Side.BID)));
		legs.add(new ArbInstructionLeg(markets.get(Side.ASK), Side.ASK, currencyPair, prices.get(Side.ASK), amounts.get(Side.ASK)));
		// TODO: implement valueCurrency and message
		return new ArbInstruction(decision, estimatedValue, legs, null);
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
