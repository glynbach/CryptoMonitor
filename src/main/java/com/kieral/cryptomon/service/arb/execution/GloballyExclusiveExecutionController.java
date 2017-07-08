package com.kieral.cryptomon.service.arb.execution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.PollingService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;
import com.kieral.cryptomon.service.arb.ArbInstructionFactory;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

/**
 *
 * Allows only one set of arbitrage executions to be in play
 *
 */
@Component
public class GloballyExclusiveExecutionController implements ExecutionController {

	private final static int MAX_PERMITTED_ARBS = 15;
	private final static BigDecimal SHORTFALL_LENIANCY = new BigDecimal("0.01");
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final ReentrantLock lock = new ReentrantLock();
	private final AtomicReference<ExecutionMonitor> currentExecution =
										new AtomicReference<ExecutionMonitor>();

	@Autowired
	ExchangeManagerService exchangeManagerService;
	@Autowired
	OrderService orderService;
	@Autowired
	BalanceService balanceService;
	@Autowired
	ArbService arbService;
	@Autowired
	BackOfficeService backOfficeService;
	@Autowired
	ExecutionConfig executionConfig;
	@Autowired
	PollingService pollingService;

	private AtomicInteger numArbs = new AtomicInteger(0);

	@Override
	public void onArbInstruction(ArbInstruction originalInstruction) {
		if (arbService.isSuspended()) {
			if (logger.isDebugEnabled())
				logger.debug("Skipping arb instruction {} as arb service is suspended", originalInstruction);
			return;
		}
		lock.lock();
		try {
			ExecutionMonitor currentExecution = this.currentExecution.get();
			if (numArbs.get() < MAX_PERMITTED_ARBS) {
				if (currentExecution == null || currentExecution.isClosed()) {
					ArbInstruction instruction = getInterest(originalInstruction);
					if (instruction != null) {
						ExecutionMonitor executionMonitor = null;
						try {
							numArbs.incrementAndGet();
							logger.info("{} vs {} executing arb instruction {}", instruction.getLeg(Side.BID).getMarket(), 
									instruction.getLeg(Side.ASK).getMarket(), instruction);
							executionMonitor = new ExecutionMonitor(orderService, balanceService, arbService, backOfficeService, 
								instruction, pollingService, executionConfig.getPollingInterval());
							this.currentExecution.set(executionMonitor);
						} catch (Exception e) {
							logger.error("Error attempting to execute arb instruction {}", instruction, e);
							if (executionMonitor != null && !executionMonitor.isClosed()) {
								logger.error("There are open orders in for instruction {} with status {} - "
										+ "suspending arb service", instruction, executionMonitor.getStatus());
								arbService.suspend(true);
							}
						}
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Skipping arb instruction {} below threshold {}", 
									originalInstruction.toSummaryString(), executionConfig.getMinValue().toPlainString());
					}
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Skipping arb instruction {} while status of running arb instruction is {}", 
								originalInstruction.toSummaryString(), currentExecution.getStatus());
				}
			} else { 
				if (logger.isDebugEnabled())
					logger.debug("Skipping arb instruction {} as number of arbs executed {} has reached max permitted {}", 
							originalInstruction.toSummaryString(), numArbs.get(), MAX_PERMITTED_ARBS);
			}
		} finally {
			lock.unlock();
		}
	}
	
	private ArbInstruction getInterest(ArbInstruction instruction) {
		if (instruction.getEstimatedValue().compareTo(executionConfig.getMinValue()) >= 0)
			return instruction;
		// check if we want to rebalance on this
		ArbInstructionLeg longLeg = instruction.getLeg(Side.BID);
		ArbInstructionLeg shortLeg = instruction.getLeg(Side.ASK);
		String longMarket = longLeg.getMarket();
		String shortMarket = shortLeg.getMarket();
		Currency baseCurrency = longLeg.getCurrencyPair().getBaseCurrency();
		Currency quotedCurrency = shortLeg.getCurrencyPair().getQuotedCurrency();
		// does the short market need the short quoted currency
		BigDecimal quotedAmountShortfall = shortLeg.getCurrencyPair().getMinDesiredQuotedBalance().subtract(balanceService.getWorkingAmount(shortMarket, quotedCurrency));
		if (quotedAmountShortfall.compareTo(SHORTFALL_LENIANCY) > 0) {
			// does the long market have surplus
			if (balanceService.getWorkingAmount(longMarket, quotedCurrency).compareTo(longLeg.getCurrencyPair().getMinDesiredQuotedBalance()) > 0) {
				BigDecimal baseAdjustedShortfall = quotedAmountShortfall.divide(shortLeg.getPrice(), 8, RoundingMode.HALF_DOWN);
				BigDecimal baseAmount = baseAdjustedShortfall.compareTo(longLeg.getAmount().getBaseAmount()) > 0 ? longLeg.getAmount().getBaseAmount() : baseAdjustedShortfall; 
				logger.info("Taking arb to rebalance {} on {} - current balance {} - original instruction {}", quotedCurrency.name(), shortMarket, 
						balanceService.getWorkingAmount(shortMarket, quotedCurrency).toPlainString(), instruction);
				return ArbInstructionFactory.createArbInstruction(instruction, baseAmount);
			}
		}
		// does the long market need the long base currency
		BigDecimal baseAmountShortfall = longLeg.getCurrencyPair().getMinDesiredBaseBalance().subtract(balanceService.getWorkingAmount(longMarket, baseCurrency));
		if (baseAmountShortfall.compareTo(SHORTFALL_LENIANCY) > 0) {
			// does the short market have surplus
			if (balanceService.getWorkingAmount(shortMarket, baseCurrency).compareTo(shortLeg.getCurrencyPair().getMinDesiredBaseBalance()) > 0) {
				logger.info("Taking arb to rebalance {} on {} - current balance {} - original instruction {}", baseCurrency.name(), longMarket, 
						balanceService.getWorkingAmount(longMarket, baseCurrency).toPlainString(), instruction);
				BigDecimal baseAmount = baseAmountShortfall.compareTo(shortLeg.getAmount().getBaseAmount()) > 0 ? shortLeg.getAmount().getBaseAmount() : baseAmountShortfall;
				return ArbInstructionFactory.createArbInstruction(instruction, baseAmount);
			}
		}
		return null;
	}
	
}
