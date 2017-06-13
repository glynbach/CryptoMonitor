package com.kieral.cryptomon.service.exchange.poloniex;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexAccountsResponse extends AccountsResponse {

	private PoloniexAccountResponse btc;
	private PoloniexAccountResponse ltc;
	private PoloniexAccountResponse eth;
	
	public PoloniexAccountResponse getBtc() {
		return btc;
	}
	
	@JsonProperty("BTC")
	public void setBtc(PoloniexAccountResponse btc) {
		this.btc = btc;
	}
	
	public PoloniexAccountResponse getLtc() {
		return ltc;
	}
	
	@JsonProperty("LTC")
	public void setLtc(PoloniexAccountResponse ltc) {
		this.ltc = ltc;
	}
	
	public PoloniexAccountResponse getEth() {
		return eth;
	}
	
	@JsonProperty("ETH")
	public void setEth(PoloniexAccountResponse eth) {
		this.eth = eth;
	}
	
}
