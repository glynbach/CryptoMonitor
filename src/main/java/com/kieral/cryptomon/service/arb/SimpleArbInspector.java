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
import com.kieral.cryptomon.service.util.Tuple2;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.LiquidityEntry;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.sided.BidAskAmount;
import com.kieral.cryptomon.model.sided.BidAskMarket;
import com.kieral.cryptomon.model.sided.BidAskPrice;
import com.kieral.cryptomon.model.trading.TradeAmount;

public class SimpleArbInspector implements ArbInspector {

	private final static ArbInstruction NO_ARB = ArbInstructionFactory.createNoArbInstruction(null);
	private final static BigDecimal NINETY_NINE = new BigDecimal("99");
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	BalanceService balanceService;
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
		if (!market1.isValid()) {
			logger.warn("Orderbook {} is invalid", market1);
			return NO_ARB;
		}
		if (!market2.isValid()) {
			logger.warn("Orderbook {} is invalid", market2);
			return NO_ARB;
		}
		if (market1.isStale(1000)) {
			if (logger.isDebugEnabled())
				logger.debug("Orderbook {} is stale - time now in millis {}", market1, System.currentTimeMillis());
			return NO_ARB;
		}
		if (market2.isStale(1000)) {
			if (logger.isDebugEnabled())
				logger.debug("Orderbook {} is stale - time now in millis {}", market2, System.currentTimeMillis());
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

	private BigDecimal getBaseAmount(LiquidityEntry entry, Side side) {
		return entry.getBidAskAmount().get(side).getBaseAmount().setScale(8, RoundingMode.HALF_DOWN);
	}

	private BigDecimal getRoundedNinetyNinePercentOf(BigDecimal amount) {
		return CommonUtils.getPercentageOf(amount, NINETY_NINE).setScale(8, RoundingMode.HALF_DOWN);
	}

	private ArbInstruction checkOpportunity(OrderBook buyBook, OrderBook sellBook, LiquidityEntry buySide, LiquidityEntry sellSide) {
		// check available balances
		// what maximum amount can we do on both sides
		BigDecimal commonAmount = getBaseAmount(buySide, Side.ASK).compareTo(getBaseAmount(sellSide, Side.BID)) > 0
						? getBaseAmount(sellSide, Side.BID) : getBaseAmount(buySide, Side.ASK);
		commonAmount = getRoundedNinetyNinePercentOf(commonAmount);
		// check sell side first
		// if we're selling to market we will be be a taker of their bid price and will be paying with balance from quoted currency
		BigDecimal sellFunds = balanceService.getWorkingAmount(sellBook.getMarket(), sellBook.getCurrencyPair().getBaseCurrency());
		if (sellFunds.compareTo(commonAmount) < 0) {
			commonAmount = sellFunds;
			commonAmount = getRoundedNinetyNinePercentOf(commonAmount);
		}
		// is this a significant amount?
		if (!orderBookConfig.isSignificant(sellBook.getMarket(), sellBook.getCurrencyPair().getBaseCurrency(), sellFunds)) {
			return NO_ARB;
		}
		// now check buy side		
		// if we're buying from market we will be be a taker of their ask price and will be paying with balance from other currency
		BigDecimal buyAmountNeeded = buySide.getBidAskPrice().get(Side.ASK).multiply(commonAmount);
		BigDecimal buyFunds = getRoundedNinetyNinePercentOf(balanceService.getWorkingAmount(buyBook.getMarket(), buyBook.getCurrencyPair().getQuotedCurrency()));
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
		return checkOpportunity(commonAmount, commonAmount, buyBook, sellBook, buySide, sellSide, false);
	}
	
	/**
	 * @param baseProfitOnGreatest 	set to true if already partially filled on an arb and have imbalanced amounts 
	 * 								the profit value will be inaccurate but will allow a rebalancing to continue
	 * @return
	 */
	private ArbInstruction checkOpportunity(BigDecimal longAmount, BigDecimal shortAmount, OrderBook buyBook, 
			     OrderBook sellBook, LiquidityEntry buySide, LiquidityEntry sellSide, boolean resolvingOpenArb) {
		// if resolving an open arb, force the decision as if on the greatest of the remaining amounts
		BigDecimal greatestAmount = longAmount.compareTo(shortAmount) > 0 ? longAmount : shortAmount;
		BigDecimal longReferenceAmount = resolvingOpenArb ? greatestAmount : longAmount;
		BigDecimal shortReferenceAmount = resolvingOpenArb ? greatestAmount : shortAmount;
		// check opportunity after fees
		BigDecimal amountBoughtWithFees = longReferenceAmount.multiply(buySide.getBidAskPrice().get(Side.ASK))
				.multiply(CommonUtils.getTradingfeeMultiplier(Side.BID, buyBook.getCurrencyPair().getTradingFee()))
				.setScale(buyBook.getCurrencyPair().getPriceScale(), RoundingMode.HALF_UP);
		BigDecimal amountSoldWithFees = shortReferenceAmount.multiply(sellSide.getBidAskPrice().get(Side.BID))
				.multiply(CommonUtils.getTradingfeeMultiplier(Side.ASK, sellBook.getCurrencyPair().getTradingFee()))
				.setScale(sellBook.getCurrencyPair().getPriceScale(), RoundingMode.HALF_DOWN);
		BigDecimal profit = amountSoldWithFees.subtract(amountBoughtWithFees);
		if (profit.compareTo(BigDecimal.ZERO) > 0) {
			return ArbInstructionFactory.createArbInstruction(rateIt(profit), profit, buyBook.getCurrencyPair().getQuotedCurrency(), 
					new Tuple2<CurrencyPair, CurrencyPair>(buyBook.getCurrencyPair(), sellBook.getCurrencyPair()),
					new BidAskPrice(buySide.getBidAskPrice().get(Side.ASK), sellSide.getBidAskPrice().get(Side.BID)), 
					new BidAskAmount(
							new TradeAmount(longAmount, buySide.getBidAskPrice().get(Side.ASK), buyBook.getCurrencyPair().getPriceScale()), 
							new TradeAmount(shortAmount, sellSide.getBidAskPrice().get(Side.BID), sellBook.getCurrencyPair().getPriceScale())), 
					new BidAskMarket(buyBook.getMarket(), sellBook.getMarket()),
					new Tuple2<OrderBook, OrderBook>(buyBook, sellBook),
					resolvingOpenArb);
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
		LiquidityEntry longBest = OrderBookManager.getBestBidAsk(longBook, longAmountRemaining);
		LiquidityEntry shortBest = OrderBookManager.getBestBidAsk(shortBook, shortAmountRemaining);
		if (longBest != null && shortBest != null && longBest.getBidAskPrice() != null && shortBest.getBidAskPrice() != null
				&& longBest.getBidAskPrice().get(Side.ASK) != null && shortBest.getBidAskPrice().get(Side.BID) != null)
			return checkOpportunity(longAmountRemaining, shortAmountRemaining, longBook, shortBook, longBest, shortBest, true);
		return NO_ARB;
	}
	
}
