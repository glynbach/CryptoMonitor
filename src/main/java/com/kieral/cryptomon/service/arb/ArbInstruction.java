package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;

public class ArbInstruction {

	public final static EnumSet<ArbDecision> SOMETHING_THERE = EnumSet.of(ArbDecision.HIGH, ArbDecision.LOW);
			
	private final ArbDecision decision;
	private final BigDecimal estimatedValue;
	private final List<ArbInstructionLeg> legs;
	private final String message;

	public ArbInstruction(ArbDecision decision, BigDecimal estimatedValue, List<ArbInstructionLeg> legs, String message) {
		this.decision = decision;
		this.estimatedValue = estimatedValue;
		this.legs = legs;
		this.message = message;
	}

	public ArbDecision getDecision() {
		return decision;
	}

	public BigDecimal getEstimatedValue() {
		return estimatedValue;
	}

	public List<ArbInstructionLeg> getLegs() {
		return legs;
	}

	public String getMessage() {
		return message;
	}

	protected static class ArbInstructionLeg {
		
		private final String market;
		private final Side side;
		private final CurrencyPair currencyPair;
		private final BigDecimal price;
		private final BigDecimal amount;
		
		public ArbInstructionLeg(String market, Side side, CurrencyPair currencyPair, BigDecimal price, BigDecimal amount) {
			this.market = market;
			this.side = side;
			this.currencyPair = currencyPair;
			this.price = price;
			this.amount = amount;
		}

		public String getMarket() {
			return market;
		}

		public Side getSide() {
			return side;
		}

		public CurrencyPair getCurrencyPair() {
			return currencyPair;
		}

		public BigDecimal getPrice() {
			return price;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		@Override
		public String toString() {
			return "ArbInstructionLeg [market=" + market + ", side=" + side + ", currencyPair=" + currencyPair
					+ ", price=" + price + ", amount=" + amount + "]";
		}
		
	}

	@Override
	public String toString() {
		return "ArbInstruction [decision=" + decision + ", estimatedValue=" + estimatedValue + ", legs=" + legs
				+ ", message=" + message + "]";
	}
	
}
