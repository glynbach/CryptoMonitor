package com.kieral.cryptomon.service.arb;

public interface ArbInstructionHandler {

	/**
	 * Returns false if the instruction handler will not accept
	 */
	boolean onArbInstruction(ArbInstruction instruction);
	
}
