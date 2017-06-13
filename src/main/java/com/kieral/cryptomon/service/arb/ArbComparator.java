package com.kieral.cryptomon.service.arb;

import java.util.Comparator;

public class ArbComparator implements Comparator<ArbInstruction> {

	@Override
	public int compare(ArbInstruction o1, ArbInstruction o2) {
		if (o1.getDecision().ordinal() != o2.getDecision().ordinal())
			return Integer.compare(o1.getDecision().ordinal(), o2.getDecision().ordinal());
		return (o1.getEstimatedValue().compareTo(o2.getEstimatedValue()) * -1);
	}

}
