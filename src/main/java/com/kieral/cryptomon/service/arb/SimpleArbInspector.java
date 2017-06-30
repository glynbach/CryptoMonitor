package com.kieral.cryptomon.service.arb;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.model.general.LiquidityEntry;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.trading.TradeAmount;

public class SimpleArbInspector implements ArbInspector {

	private final static ArbInstruction NO_ARB = ArbInstructionFactory.createNoArbInstruction(null);
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	BalanceService balanceHandler;
	@Autowired
	OrderBookManager orderBookManager;
	@Autowired
	OrderBookConfig orderBookConfig;
	
	@Override
	public ArbInstruction examine(OrderBook market1, OrderBook market2) {
		if (market1.getMarket().equals(market2.getMarket())) {
			logger.warn("Given arb to examine for the same markets {} {}", market1, market2);
			return NO_ARB;
		}
		if (!market1.getCurrencyPair().getName().equals(market2.getCurrencyPair().getName())) {
			logger.warn("Given arb to examine for different currencies {} {}", market1, market2);
			return NO_ARB;
		}
		// basic top of the books check
		LiquidityEntry market1Best = orderBookManager.getBestBidAsk(market1);
		LiquidityEntry market2Best = orderBookManager.getBestBidAsk(market2);
		// looking for a straight up bid / ask cross
		if (!isPositive(market1Best.getBidAskPrice().get(Side.BID))
				|| !isPositive(market1Best.getBidAskPrice().get(Side.ASK))
				|| !isPositive(market2Best.getBidAskPrice().get(Side.BID))
				|| !isPositive(market2Best.getBidAskPrice().get(Side.ASK))) {
			return NO_ARB;
		}
		if (market1Best.getBidAskPrice().get(Side.BID).compareTo(market2Best.getBidAskPrice().get(Side.ASK)) > 0) {
			// buying from market2 may provide an opportunity
			return checkOpportunity(market2, market1, market2Best, market1Best);
		} else if (market2Best.getBidAskPrice().get(Side.BID).compareTo(market1Best.getBidAskPrice().get(Side.ASK)) > 0) {
			// buying from market1 may provide an opportunity
			return checkOpportunity(market1, market2, market1Best, market2Best);
		} else {
			return NO_ARB;
		}
	}

	private ArbInstruction checkOpportunity(OrderBook buyBook, OrderBook sellBook, LiquidityEntry buySide, LiquidityEntry sellSide) {
		// check available balances
		// what maximum amount can we do on both sides
		BigDecimal commonAmount = buySide.getBidAskAmount().get(Side.ASK).getBaseAmount()
				.compareTo(sellSide.getBidAskAmount().get(Side.BID).getBaseAmount()) > 0
						? sellSide.getBidAskAmount().get(Side.BID).getBaseAmount() 
						: buySide.getBidAskAmount().get(Side.ASK).getBaseAmount(); 
		// check sell side first
		// if we're selling to market we will be be a taker of their bid price and will be paying with balance from quoted currency
		BigDecimal sellFunds = balanceHandler.getWorkingAmount(sellBook.getMarket(), sellBook.getCurrencyPair().getBaseCurrency());
		if (sellFunds.compareTo(commonAmount) < 0) {
			commonAmount = sellFunds;
		}
		// is this a significant amount?
		if (!orderBookConfig.isSignificant(sellBook.getMarket(), sellBook.getCurrencyPair().getBaseCurrency(), sellFunds)) {
			return NO_ARB;
		}
		// now check buy side		
		// if we're buying from market we will be be a taker of their ask price and will be paying with balance from other currency
		BigDecimal buyAmountNeeded = buySide.getBidAskPrice().get(Side.ASK).multiply(commonAmount);
		BigDecimal buyFunds = balanceHandler.getWorkingAmount(buyBook.getMarket(), buyBook.getCurrencyPair().getQuotedCurrency());
		buyFunds = buyFunds.compareTo(buyAmountNeeded) < 0 ? buyFunds : buyAmountNeeded;
		// is this still a significant amount?
		if (!orderBookConfig.isSignificant(buyBook.getMarket(), buyBook.getCurrencyPair().getQuotedCurrency(), buyFunds)) {
			return NO_ARB;
		}
		if (buyFunds.compareTo(buyAmountNeeded) != 0) {
			// adjust back the smallestCommonAmount and sell funds
			commonAmount = buyFunds.divide(buySide.getBidAskPrice().get(Side.ASK), buyBook.getCurrencyPair().getPriceScale(), RoundingMode.HALF_DOWN);
			// check new sell funds are significant 
			if (!orderBookConfig.isSignificant(sellBook.getMarket(), sellBook.getCurrencyPair().getBaseCurrency(), sellFunds)) {
				return NO_ARB;
			}
		}
		return checkOpportunity(commonAmount, commonAmount, buyBook, sellBook, buySide, sellSide);
	}
	
	private ArbInstruction checkOpportunity(BigDecimal longAmount, BigDecimal shortAmount, OrderBook buyBook, 
			     OrderBook sellBook, LiquidityEntry buySide, LiquidityEntry sellSide) {
		// check opportunity after fees
		BigDecimal amountBoughtWithFees = longAmount.multiply(buySide.getBidAskPrice().get(Side.ASK))
				.multiply(CommonUtils.getTradingfeeMultiplier(buyBook.getCurrencyPair().getTradingFee()))
				.setScale(buyBook.getCurrencyPair().getPriceScale(), RoundingMode.HALF_UP);
		BigDecimal amountSoldWithFees = shortAmount.multiply(sellSide.getBidAskPrice().get(Side.BID))
				.multiply(CommonUtils.getTradingfeeMultiplier(sellBook.getCurrencyPair().getTradingFee()))
				.setScale(sellBook.getCurrencyPair().getPriceScale(), RoundingMode.HALF_DOWN);
		BigDecimal profit = amountSoldWithFees.subtract(amountBoughtWithFees);
		if (profit.compareTo(BigDecimal.ZERO) > 0) {
			return ArbInstructionFactory.createArbInstruction(rateIt(profit), profit, buyBook.getCurrencyPair().getQuotedCurrency(), buyBook.getCurrencyPair(),
					new BidAskPrice(buySide.getBidAskPrice().get(Side.ASK), sellSide.getBidAskPrice().get(Side.BID)), 
					new BidAskAmount(
							new TradeAmount(longAmount, buySide.getBidAskPrice().get(Side.ASK), buyBook.getCurrencyPair().getPriceScale()), 
							new TradeAmount(shortAmount, sellSide.getBidAskPrice().get(Side.BID), buyBook.getCurrencyPair().getPriceScale())), 
					new BidAskMarket(buyBook.getMarket(), sellBook.getMarket()));
		}
		if (logger.isDebugEnabled())
			logger.debug("No profit in {} USD after fees for {} and {} prices {} amd {}", profit.multiply(new BigDecimal("2500")).setScale(4, RoundingMode.HALF_UP), buyBook.getMarket(), sellBook.getMarket(), buySide, sellSide);
		return NO_ARB;	
	}
	
	private boolean isPositive(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) > 0;
	}
	
	// TODO: do this properly
	private final static BigDecimal LOW = new BigDecimal("0.05"); 
	private ArbDecision rateIt(BigDecimal profit) {
		if (LOW.compareTo(profit) > 0)
			return ArbDecision.LOW;
		else
			return ArbDecision.HIGH;
	}

	@Override
	public ArbInstruction resolve(OrderBook longBook, OrderBook shortBook, BigDecimal longAmountRemaining,
			BigDecimal shortAmountRemaining) {
		LiquidityEntry longBest = orderBookManager.getBestBidAsk(longBook, longAmountRemaining);
		LiquidityEntry shortBest = orderBookManager.getBestBidAsk(shortBook, shortAmountRemaining);
		if (longBest != null && shortBest != null)
			return checkOpportunity(longAmountRemaining, shortAmountRemaining, longBook, shortBook, longBest, shortBest);
		return NO_ARB;
	}
	
}