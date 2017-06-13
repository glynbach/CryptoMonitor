package com.kieral.cryptomon.messaging;

import com.kieral.cryptomon.service.arb.ArbInstruction;

public class ArbStatusMessage {

	private ArbInstruction arbInstruction;

	public ArbStatusMessage() {
	}
	
	public ArbStatusMessage(ArbInstruction arbInstruction) {
		super();
		this.arbInstruction = arbInstruction;
	}

	public ArbInstruction getArbInstruction() {
		return arbInstruction;
	}

	public void setArbInstruction(ArbInstruction arbInstruction) {
		this.arbInstruction = arbInstruction;
	}

	@Override
	public String toString() {
		return "ArbStatusMessage [arbInstruction=" + arbInstruction + "]";
	}
	
}
