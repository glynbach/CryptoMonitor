package com.kieral.cryptomon.service.exchange.yobit.payload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class YobitAccountsResponse implements AccountsResponse {

	private List<Object[]> funds;
	
	
	public List<Object[]> getFunds() {
		return funds;
	}


	public void setFunds(List<Object[]> funds) {
		this.funds = funds;
	}


	@Override
	public List<? extends AccountResponse> getAccountResponses() {
		if (funds == null)
			return Collections.<AccountResponse>emptyList();
		else {
			List<AccountResponse> rtn = new ArrayList<AccountResponse>();
			for (Object[] fund : funds) {
				if (fund.length != 2)
					throw new IllegalStateException(String.format("Excepted fund to be in form currency: amount, but got %s", Arrays.asList(fund)));
				rtn.add(new AccountResponse() {
					@Override
					public String getAccountId() {
						return fund[0] + "-account";
					}
					@Override
					public String getAccountAddress() {
						return null;
					}
					@Override
					public Currency getAccountCurrency() {
						return Currency.valueOf(String.valueOf(fund[0]).toUpperCase());
					}
					@Override
					public BigDecimal getAvailableBalance() {
						return new BigDecimal(String.valueOf(fund[1]));
					}
					@Override
					public BigDecimal getPendingBalance() {
						return new BigDecimal(String.valueOf(fund[1]));
					}});
			}
			return rtn;
		}
	}


	@Override
	public String toString() {
		return "YobitAccountsResponse [funds=" + funds + "]";
	}
	
}
