package com.kieral.cryptomon.service.exchange.poloniex;

import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.codec.Hex;

import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class PoloniexSecurityModule extends ServiceSecurityModule {

	Mac mac;
	
	public PoloniexSecurityModule(ServiceExchangeProperties properties) {
		super(properties);
	}

	@Override
	public void initialise() throws Exception {
		if (apiSecret == null || apiSecret.isEmpty())
			throw new IllegalStateException("Detected no value for apiSecret so cannot initialise security module");
        SecretKey secretKey = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA512");
        mac = Mac.getInstance("HmacSHA512");
        mac.init(secretKey);
	}

	@Override
	public String appendApiRequestPathEntries(String requestPath) {
		return requestPath;
	}

	@Override
	public void appendApiPostParameterEntries(Map<String, String> postParameters) {
		if (postParameters == null)
			throw new IllegalStateException("postParameters can not be null");
		postParameters.put("nonce", String.valueOf(millisNonce.incrementAndGet()));
	}

	@Override
	public HttpHeaders sign(long timestamp, Method method, String requestPath, String body) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Key", apiKey);
		headers.add("Sign", new String(Hex.encode(mac.doFinal((body).getBytes()))));
		headers.add("Content-Type", "application/x-www-form-urlencoded");
		return headers;
	}

}
