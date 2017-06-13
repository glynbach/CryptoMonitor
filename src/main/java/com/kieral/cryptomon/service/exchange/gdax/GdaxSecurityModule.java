package com.kieral.cryptomon.service.exchange.gdax;

import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Base64Utils;

import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class GdaxSecurityModule extends ServiceSecurityModule {

	private Mac mac;
	
	public GdaxSecurityModule(ServiceExchangeProperties properties) {
		super(properties);
	}

	@Override
	public void initialise() throws Exception {
		if (apiSecret == null || apiSecret.isEmpty())
			throw new IllegalStateException("Detected no value for apiSecret so cannot initialise security module");
        SecretKey secretKey = new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256");
        mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKey);
	}

	@Override
	public HttpHeaders sign(long timestamp, Method method, String requestPath, String body) throws Exception {
		String data = (timestamp / 1000) + method.name() + requestPath + (body == null ? "" : body); 
		HttpHeaders headers = new HttpHeaders();
		headers.add("CB-ACCESS-KEY", apiKey);
		headers.add("CB-ACCESS-SIGN", new String(Base64Utils.encodeToString(mac.doFinal(data.getBytes()))));
		headers.add("CB-ACCESS-TIMESTAMP", String.valueOf(timestamp/1000));
		headers.add("CB-ACCESS-PASSPHRASE", String.valueOf(timestamp/1000));
		return headers;
	}

	@Override
	public String appendApiRequestPathEntries(String requestPath) {
		return requestPath;
	}

	@Override
	public void appendApiPostParameterEntries(Map<String, String> postParameters) {
	}

}
