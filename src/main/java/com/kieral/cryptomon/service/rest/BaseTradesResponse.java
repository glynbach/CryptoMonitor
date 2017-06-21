package com.kieral.cryptomon.service.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.service.util.TradingUtils;

public class BaseTradesResponse implements TradesResponse {

	private final static Function<TradeResponse, Trade> responseToTrade = new Function<TradeResponse, Trade>() {
    	public Trade apply(TradeResponse tradeResponse) {
    		if (tradeResponse == null)
    			return null;
    		return new Trade(tradeResponse.getTradeId(), tradeResponse.getRate(), tradeResponse.getAmount(), 
    				tradeResponse.getFee(), tradeResponse.isFeeInQuotedCurrency(), tradeResponse.getTradeTime());
    	}
    };

	private final List<? extends TradeResponse> tradeResponses;
	
	public BaseTradesResponse(List<? extends TradeResponse> tradeResponses) {
		super();
		this.tradeResponses = tradeResponses;
	}

	@Override
	public List<? extends TradeResponse> getTradeResponses() {
		return tradeResponses;
	}

	@Override
	public BigDecimal getFilledAmount() {
		return TradingUtils.getFilledAmount(tradeResponses);
	}

	@Override
	public boolean isPlaceholder() {
		return false;
	}

	@Override
	public int getNumTrades() {
		return tradeResponses == null ? 0 : tradeResponses.size();
	}

	@Override
	public List<Trade> getTrades() {
		return tradeResponses == null ? null : tradeResponses.stream().map(responseToTrade).collect(Collectors.<Trade>toList());
	}

	@Override
	public String toString() {
		return "BaseTradeResponse [tradeResponses=" + tradeResponses + "]";
	}

}
