package com.kieral.cryptomon.service.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpUtils {

	private static ObjectMapper mapper = new ObjectMapper();

	public static String getResponseAsString(String getUrl) throws IOException {
		HttpURLConnection connection = null;
		InputStreamReader in = null;
        StringBuilder sb = new StringBuilder();
        URL url = new URL(getUrl);
		try {
			connection = (HttpURLConnection)url.openConnection();
			in = new InputStreamReader(connection.getInputStream());
			BufferedReader br = new BufferedReader(in);
			if(br!=null){
			    int  cp;
			    while((cp=br.read())!=-1){
			        sb.append((char) cp);
			    }
			    br.close();
			}
			return sb.toString();
		} finally {
			try {
				in.close();
			} catch (Exception e) {}
			try {
				connection.disconnect();
			} catch (Exception e) {}
		}
	}

	public static JsonNode getResponseAsJson(String getUrl) throws JsonProcessingException, IOException {
		String response = getResponseAsString(getUrl);
		if (response == null)
			return null;
    	JsonNode json = mapper.readTree(response);
    	return json;
	}
	
	public static void main(String[] args) throws JsonProcessingException, IOException {
		System.out.println(getResponseAsJson("https://poloniex.com/public?command=returnOrderBook&currencyPair=BTC_LTC&depth=10"));
	}
}
