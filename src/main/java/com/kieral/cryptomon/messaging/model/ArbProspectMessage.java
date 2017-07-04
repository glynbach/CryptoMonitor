package com.kieral.cryptomon.messaging.model;

import java.util.List;

public class ArbProspectMessage {

	private List<MarketArbProspectMessage> prospects;

	public ArbProspectMessage() {
	}
	
	public ArbProspectMessage(List<MarketArbProspectMessage> prospects) {
		this.prospects = prospects;
	}

	public List<MarketArbProspectMessage> getProspects() {
		return prospects;
	}

	public void setProspects(List<MarketArbProspectMessage> prospects) {
		this.prospects = prospects;
	}

	@Override
	public String toString() {
		return "ArbProspectMessage [prospects=" + prospects + "]";
	}
	
}
