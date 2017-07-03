package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.service.util.Tuple2;

public class BackOfficeServiceImpl implements BackOfficeService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final List<BackOfficeListener> listeners = new CopyOnWriteArrayList<BackOfficeListener>();
	
	@Autowired
	private ExchangeManagerService exchangeManagerService;
	@Autowired
	private ArbService arbService;

	private final ExecutorService processor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "BackOfficeDaemon");
			thread.setDaemon(true);
			return thread;
		}});
	
	@Override
	public void onExecutionCompletion(ArbInstruction instruction, List<Order> longOrders, List<Order> shortOrders,
			boolean interventionRequired) {
		// restore balances
		try {
			exchangeManagerService.updateBalances(instruction.getLeg(Side.BID).getMarket(), true);
			exchangeManagerService.updateBalances(instruction.getLeg(Side.ASK).getMarket(), true);
		} catch (Exception e) {
			logger.error("Error updating balances after arb {} - suspending arb service", instruction, e);
			arbService.suspend(true);
			interventionRequired = true;
		}
		if (interventionRequired) {
			// TODO: take action
			// send alerts
		}
		processor.submit(new ProcessCompletedInstructionTask(instruction, longOrders, shortOrders, interventionRequired));
	}
	
	public void registerListener(BackOfficeListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	private Tuple2<BigDecimal, BigDecimal> getTradedAmounts(Order order) {
		BigDecimal baseAmountTotal = BigDecimal.ZERO;
		BigDecimal quotedAmountTotal = BigDecimal.ZERO;
		for (Trade trade : order.getTrades()) {
			BigDecimal baseAmount = trade.getAmount();
			if (!trade.isFeeOnQuotedCurrency())
				baseAmount = baseAmount.multiply(CommonUtils.getTradingfeeMultiplier(order.getCurrencyPair().getTradingFee())); 
			BigDecimal quotedAmount = trade.getAmount().multiply(trade.getRate())
					.setScale(8, RoundingMode.HALF_DOWN);
			if (!trade.isFeeOnQuotedCurrency())
				quotedAmount = quotedAmount.multiply(CommonUtils.getTradingfeeMultiplier(order.getCurrencyPair().getTradingFee()));
			baseAmountTotal = baseAmountTotal.add(baseAmount);
			quotedAmountTotal = quotedAmountTotal.add(quotedAmount);
		}
		return new Tuple2<BigDecimal, BigDecimal>(baseAmountTotal, quotedAmountTotal);
	}
	
	class ProcessCompletedInstructionTask implements Runnable {

		final ArbInstruction instruction;
		final List<Order> longOrders;
		final List<Order> shortOrders;
		final boolean interventionRequired;
		
		ProcessCompletedInstructionTask(ArbInstruction instruction, List<Order> longOrders, List<Order> shortOrders,
				boolean interventionRequired) {
			this.instruction = instruction;
			this.longOrders = longOrders;
			this.shortOrders = shortOrders;
			this.interventionRequired = interventionRequired;
		}
		
		@Override
		public void run() {
			// evaluate p&l
			BigDecimal netBaseAmount = BigDecimal.ZERO;
			BigDecimal netQuotedAmount = BigDecimal.ZERO;
			List<Trade> longTrades = new ArrayList<Trade>();
			List<Trade> shortTrades = new ArrayList<Trade>();
			// long trades
			if (longOrders != null) {
				for (Order order : longOrders) {
					if (order.getTrades() != null) {
						Tuple2<BigDecimal, BigDecimal> tradeAmounts = getTradedAmounts(order);
						netBaseAmount = netBaseAmount.add(tradeAmounts.getA());
						netQuotedAmount = netQuotedAmount.add(tradeAmounts.getB().negate());
						longTrades.addAll(order.getTrades());
					}
				}
			}
			if (shortOrders != null) {
				for (Order order : shortOrders) {
					if (order.getTrades() != null) {
						Tuple2<BigDecimal, BigDecimal> tradeAmounts = getTradedAmounts(order);
						netBaseAmount = netBaseAmount.add(tradeAmounts.getA().negate());
						netQuotedAmount = netQuotedAmount.add(tradeAmounts.getB());
						shortTrades.addAll(order.getTrades());
					}
				}
			}
			BackOfficeArbSummary summary = new BackOfficeArbSummary(instruction, longTrades, shortTrades,
					netBaseAmount, netQuotedAmount, interventionRequired);
			logger.info("BackOffice processing of arb instruction complete with summary {}", summary);
			listeners.forEach(listener -> {
				try {
					listener.onArbCompleted(summary);
				} catch (Exception e) {
					logger.error("Listener errored on processing summary {}", summary, e);
				}
			});
		}
	}

}
