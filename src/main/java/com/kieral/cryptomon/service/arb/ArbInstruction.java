package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.util.EnumSet;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.SidedValue;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.service.util.Tuple2;

public class ArbInstruction {

	public final static EnumSet<ArbDecision> SOMETHING_THERE = EnumSet.of(ArbDecision.HIGH, ArbDecision.LOW);
			
	private final ArbDecision decision;
	private final BigDecimal estimatedValue;
	private final SidedArbInstructionLeg legs;
	private final String message;
	private final Tuple2<OrderBook, OrderBook> orderBooks;

	public ArbInstruction(ArbDecision decision, BigDecimal estimatedValue, SidedArbInstructionLeg legs, String message,
			Tuple2<OrderBook, OrderBook> orderBooks) {
		this.decision = decision;
		this.estimatedValue = estimatedValue;
		this.legs = legs;
		this.message = message;
		this.orderBooks = orderBooks;
	}

	public ArbDecision getDecision() {
		return decision;
	}

	public BigDecimal getEstimatedValue() {
		return estimatedValue;
	}

	public SidedArbInstructionLeg getLegs() {
		return legs;
	}

	public ArbInstructionLeg getLeg(Side side) {
		return legs.get(side);
	}

	public String getMessage() {
		return message;
	}

	public Tuple2<OrderBook, OrderBook> getOrderBooks() {
		return orderBooks;
	}

	public static class SidedArbInstructionLeg extends SidedValue<ArbInstructionLeg> {

		public SidedArbInstructionLeg(ArbInstructionLeg bidLeg, ArbInstructionLeg askLeg) {
			super(bidLeg, askLeg);
		}

		@Override
		public String toString() {
			return "SidedArbInstructionLeg [getBidValue()=" + getBidValue() + ", getAskValue()=" + getAskValue() + "]";
		}
		
	}

	public static class ArbInstructionLeg {
		
		private final String market;
		private final Side side;
		private final CurrencyPair currencyPair;
		private final BigDecimal price;
		private final TradeAmount amount;
		
		public ArbInstructionLeg(String market, Side side, CurrencyPair currencyPair, BigDecimal price, TradeAmount amount) {
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

		public TradeAmount getAmount() {
			return amount;
		}

		@Override
		public String toString() {
			return "ArbInstructionLeg [market=" + market + ", side=" + side + ", currencyPair=" + currencyPair
					+ ", price=" + price + ", amount=" + amount + "]";
		}
		
	}

	public String toSummaryString() {
		return "decision=" + decision + " estimatedValue=" + estimatedValue + 
				" -" + legs.get(Side.BID).getAmount().getQuotedAmount().toPlainString() + legs.get(Side.BID).getCurrencyPair().getQuotedCurrency() +
					" @" + legs.get(Side.BID).getMarket() +
				" +" + legs.get(Side.ASK).getAmount().getBaseAmount().toPlainString() + legs.get(Side.ASK).getCurrencyPair().getBaseCurrency() +
					" @" + legs.get(Side.ASK).getMarket();
	}
	
	@Override
	public String toString() {
		return "ArbInstruction [decision=" + decision + ", estimatedValue=" + estimatedValue + ", legs=" + legs
				+ ", message=" + message + ", orderBooks=" + orderBooks + "]";
	}
	
}
