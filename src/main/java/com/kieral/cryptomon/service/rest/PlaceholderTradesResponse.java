package com.kieral.cryptomon.service.rest;

import java.util.Arrays;
import java.util.Collections;

public class PlaceholderTradesResponse extends BaseTradesResponse {

	public PlaceholderTradesResponse(TradeResponse tradeResponse) {
		super(tradeResponse == null ? Collections.emptyList() : Arrays.asList(new TradeResponse[]{tradeResponse}));
	}

	@Override
	public boolean isPlaceholder() {
		return true;
	}

	@Override
	public String toString() {
		return "PlaceholderTradesResponse [toString()=" + super.toString() + "]";
	}
	
}
