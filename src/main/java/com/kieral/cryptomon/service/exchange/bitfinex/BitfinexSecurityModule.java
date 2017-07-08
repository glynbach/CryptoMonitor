package com.kieral.cryptomon.service.exchange.bitfinex;

import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.kieral.cryptomon.service.exception.SecurityModuleException;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class BitfinexSecurityModule extends ServiceSecurityModule {

	private Mac mac;
	
	public BitfinexSecurityModule(ServiceExchangeProperties properties) {
		super(properties);
	}

	@Override
	public void initialise() throws SecurityModuleException {
		try {
			if (apiSecret == null || apiSecret.isEmpty())
				throw new IllegalStateException("Detected no value for apiSecret so cannot initialise security module");
	        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(apiSecret), "HmacSHA384");
	        mac = Mac.getInstance("HmacSHA384");
	        mac.init(secretKey);
		} catch (Exception e) {
			throw new SecurityModuleException("Exception initialising HmacSHA384", e);
		}
	}

	@Override
	public HttpHeaders sign(long timestamp, HttpMethod method, String requestPath, String body) throws SecurityModuleException {
		try {
			String payload = Base64.getEncoder().encodeToString(body.getBytes());
			HttpHeaders headers = new HttpHeaders();
	        headers.add("accept", "application/json");
	        headers.add("content-type", "application/json");		
			headers.add("X-BFX-APIKEY", apiKey);
			headers.add("X-BFX-PAYLOAD", payload);
			headers.add("X-BFX-SIGNATURE", new String(mac.doFinal(payload.getBytes())));
			return headers;
		} catch (Exception e) {
			throw new SecurityModuleException(String.format("Exception signing and encoding %s", body), e);
		}
	}

	@Override
	public String appendApiRequestPathEntries(String requestPath) {
		return requestPath;
	}

	@Override
	public void appendApiPostParameterEntries(Map<String, String> postParameters) {
	}

}
