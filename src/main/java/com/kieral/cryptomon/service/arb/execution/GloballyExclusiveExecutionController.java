package com.kieral.cryptomon.service.arb.execution;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

/**
 *
 * Allows only one set of arbitrage executions to be in play
 *
 */
@Component
public class GloballyExclusiveExecutionController implements ExecutionController {

	private final static int MAX_PERMITTED_ARBS = 1;
	
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

	private AtomicInteger numArbs = new AtomicInteger(0);

	@Override
	public void onArbInstruction(ArbInstruction instruction) {
		if (arbService.isSuspended()) {
			if (logger.isDebugEnabled())
				logger.debug("Skipping arb instruction {} as arb service is suspended", instruction);
			return;
		}
		lock.lock();
		try {
			ExecutionMonitor currentExecution = this.currentExecution.get();
			if (numArbs.get() < MAX_PERMITTED_ARBS) {
				if (currentExecution == null || currentExecution.isDone()) {
					if (instruction.getEstimatedValue().compareTo(executionConfig.getMinValue()) >= 0) {
						ExecutionMonitor executionMonitor = null;
						try {
							numArbs.incrementAndGet();
							logger.info("{} vs {} executing arb instruction {}", instruction.getLeg(Side.BID).getMarket(), 
									instruction.getLeg(Side.ASK).getMarket(), instruction);
							executionMonitor = new ExecutionMonitor(orderService, balanceService, arbService, backOfficeService, 
								instruction, executionConfig.getPollingInterval());
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
							logger.debug("Skipping arb instruction {} with estimated profit {} below threshold {}", 
									instruction, instruction.getEstimatedValue().toPlainString(), executionConfig.getMinValue().toPlainString());
					}
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Skipping arb instruction {} while status of running arb instruction is {}", 
								instruction, currentExecution.getStatus());
				}
			} else { 
				if (logger.isDebugEnabled())
					logger.debug("Skipping arb instruction {} as number of arbs executed {} has reached max permitted {}", 
							instruction, numArbs.get(), MAX_PERMITTED_ARBS);
			}
		} finally {
			lock.unlock();
		}
	}
	
	
}
