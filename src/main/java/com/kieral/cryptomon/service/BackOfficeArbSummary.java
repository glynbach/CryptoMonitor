package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.util.List;

import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.service.arb.ArbInstruction;

public class BackOfficeArbSummary {

	private final ArbInstruction arbInstruction;
	private final List<Trade> longTrades;
	private final List<Trade> shortTrades;
	private final BigDecimal netBaseAmount;
	private final BigDecimal netQuotedAmount;
	private final boolean interventionNeeded;
	
	public BackOfficeArbSummary(ArbInstruction arbInstruction, List<Trade> longTrades, List<Trade> shortTrades,
			BigDecimal netBaseAmount, BigDecimal netQuotedAmount, boolean interventionNeeded) {
		this.arbInstruction = arbInstruction;
		this.longTrades = longTrades;
		this.shortTrades = shortTrades;
		this.netBaseAmount = netBaseAmount;
		this.netQuotedAmount = netQuotedAmount;
		this.interventionNeeded = interventionNeeded;
	}

	public ArbInstruction getArbInstruction() {
		return arbInstruction;
	}

	public List<Trade> getLongTrades() {
		return longTrades;
	}

	public List<Trade> getShortTrades() {
		return shortTrades;
	}

	public BigDecimal getNetBaseAmount() {
		return netBaseAmount;
	}

	public BigDecimal getNetQuotedAmount() {
		return netQuotedAmount;
	}

	public boolean isInterventionNeeded() {
		return interventionNeeded;
	}

	@Override
	public String toString() {
		return "BackOfficeArbSummary [arbInstruction=" + arbInstruction + ", longTrades=" + longTrades
				+ ", shortTrades=" + shortTrades + ", netBaseAmount=" + netBaseAmount + ", netQuotedAmount="
				+ netQuotedAmount + ", interventionNeeded=" + interventionNeeded + "]";
	}
	
}
