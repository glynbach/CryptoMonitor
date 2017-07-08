package com.kieral.cryptomon.service.exchange.yobit;

import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.codec.Hex;

import com.kieral.cryptomon.service.exception.SecurityModuleException;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class YobitSecurityModule extends ServiceSecurityModule {

	Mac mac;
	
	public YobitSecurityModule(ServiceExchangeProperties properties) {
		super(properties);
	}

	@Override
	public void initialise() throws SecurityModuleException {
		try {
			if (apiSecret == null || apiSecret.isEmpty())
				throw new IllegalStateException("Detected no value for apiSecret so cannot initialise security module");
	        SecretKey secretKey = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA512");
	        mac = Mac.getInstance("HmacSHA512");
	        mac.init(secretKey);
		} catch (Exception e) {
			throw new SecurityModuleException("Exception initialising HmacSHA512", e);
		}
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
	public HttpHeaders sign(long timestamp, HttpMethod method, String requestPath, String body) throws SecurityModuleException {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Key", apiKey);
			headers.add("Sign", new String(Hex.encode(mac.doFinal((body).getBytes()))));
			headers.add("Content-Type", "application/x-www-form-urlencoded");
			return headers;
		} catch (Exception e) {
			throw new SecurityModuleException(String.format("Exception signing and hashing %s", body), e);
		}
	}

}
