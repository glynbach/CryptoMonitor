package com.kieral.cryptomon.service.exchange.poloniex.payload;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PoloniexAccountsResponse implements AccountsResponse {

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

	@Override
	public List<AccountResponse> getAccountResponses() {
		List<AccountResponse> accountResponses = new ArrayList<AccountResponse>();
		// TODO: get wallet addresses and set them here too
		if (btc != null) {
			btc.setCurrency(Currency.BTC);
			accountResponses.add(btc);
		}
		if (ltc != null) {
			ltc.setCurrency(Currency.LTC);
			accountResponses.add(ltc);
		}
		if (eth != null) {
			eth.setCurrency(Currency.ETH);
			accountResponses.add(eth);
		}
		return accountResponses;
	}

	@Override
	public String toString() {
		return "PoloniexAccountsResponse [btc=" + btc + ", ltc=" + ltc + ", eth=" + eth + "]";
	}
	
}
