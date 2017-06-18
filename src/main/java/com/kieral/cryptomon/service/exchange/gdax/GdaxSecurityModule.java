package com.kieral.cryptomon.service.exchange.gdax;

import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;

import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exception.SecurityModuleException;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class GdaxSecurityModule extends ServiceSecurityModule {

	private Mac mac;
	
	public GdaxSecurityModule(ServiceExchangeProperties properties) {
		super(properties);
	}

	@Override
	public void initialise() throws SecurityModuleException {
		try {
			if (apiSecret == null || apiSecret.isEmpty())
				throw new IllegalStateException("Detected no value for apiSecret so cannot initialise security module");
	        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(apiSecret), "HmacSHA256");
	        mac = Mac.getInstance("HmacSHA256");
	        mac.init(secretKey);
		} catch (Exception e) {
			throw new SecurityModuleException("Exception initialising HmacSHA256", e);
		}
	}

	@Override
	public HttpHeaders sign(long timestamp, Method method, String requestPath, String body) throws SecurityModuleException {
		String data = (timestamp / 1000) + method.name() + requestPath + (body == null ? "" : body); 
		try {
			HttpHeaders headers = new HttpHeaders();
	        headers.add("accept", "application/json");
	        headers.add("content-type", "application/json");		
			headers.add("CB-ACCESS-KEY", apiKey);
			headers.add("CB-ACCESS-SIGN", new String(Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes()))));
			headers.add("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp/1000));
			headers.add("CB-ACCESS-PASSPHRASE", apiPassphrase);
			return headers;
		} catch (Exception e) {
			throw new SecurityModuleException(String.format("Exception signing and encoding %s", data), e);
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
