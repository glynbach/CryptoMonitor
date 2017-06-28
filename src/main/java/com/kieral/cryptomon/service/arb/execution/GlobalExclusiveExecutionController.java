package com.kieral.cryptomon.service.arb.execution;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import com.kieral.cryptomon.service.arb.ArbInstruction;

/**
 *
 * Allows only one set of arbitrage executions to be in play
 *
 */
public class GlobalExclusiveExecutionController implements ExecutionController {

	private final ReentrantLock lock = new ReentrantLock();
	private final AtomicReference<ArbInstruction> runningInstruction =
										new AtomicReference<ArbInstruction>();
	@Override
	public boolean onArbInstruction(ArbInstruction instruction) {
		lock.lock();
		try {
			
		} finally {
			lock.unlock();
		}
		return false;
	}
	
}
