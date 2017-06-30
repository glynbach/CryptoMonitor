package com.kieral.cryptomon.service.arb.execution;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

/**
 *
 * Allows only one set of arbitrage executions to be in play
 *
 */
public class GloballyExclusiveExecutionController implements ExecutionController {

	private final ReentrantLock lock = new ReentrantLock();
	private final AtomicReference<ExecutionMonitor> currentExecution =
										new AtomicReference<ExecutionMonitor>();

	@Autowired
	ExchangeManagerService exchangeManagerService;
	@Autowired
	OrderService orderService;
	
	@Override
	public boolean onArbInstruction(ArbInstruction instruction) {
		lock.lock();
		try {
			ExecutionMonitor currentExecution = this.currentExecution.get();
			if (currentExecution.isDone()) {
				// TODO: proceed with new one
			} else if (currentExecution.isClosed()) {
				// TODO: decide if we need this state
			}
			//  TODO: do we want to discover or force a state from this existing one, it might be 
			// failed or taking too long
		} finally {
			lock.unlock();
		}
		return false;
	}
	
	
}
