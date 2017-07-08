package com.kieral.cryptomon.service.exchange.bitfine.payload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kieral.cryptomon.service.rest.AccountResponse;
import com.kieral.cryptomon.service.rest.AccountsResponse;

public class BitfinexAccountsResponse extends ArrayList<BitfinexAccountResponse> implements AccountsResponse {

	private static final long serialVersionUID = 5122354084113343533L;

	@Override
	public List<? extends AccountResponse> getAccountResponses() {
		return this.subList(0, this.size());
	}
	
	@Override
	public String toString() {
		return "BitfinexAccountsResponse [getAccountResponses()=" + getAccountResponses() + "]";
	}

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {
		String json = "[{  \"type\":\"deposit\",  \"currency\":\"btc\",  \"amount\":\"0.0\",  \"available\":\"0.0\"},{  \"type\":\"deposit\",  \"currency\":\"usd\",  \"amount\":\"1.0\",  \"available\":\"1.0\"},{  \"type\":\"exchange\",  \"currency\":\"btc\", \"amount\":\"1\",  \"available\":\"1\"},{  \"type\":\"exchange\",  \"currency\":\"usd\",  \"amount\":\"1\",  \"available\":\"1\"},{  \"type\":\"trading\",  \"currency\":\"btc\",  \"amount\":\"1\",  \"available\":\"1\"},{  \"type\":\"trading\", \"currency\":\"usd\",  \"amount\":\"1\",  \"available\":\"1\"}]";
		System.out.println(new ObjectMapper().readValue(json, BitfinexAccountsResponse.class));
	}
}
