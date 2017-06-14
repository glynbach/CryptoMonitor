package com.kieral.cryptomon.service.exchange.bittrex;

import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.codec.Hex;

import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ServiceSecurityModule;

public class BittrexSecurityModule extends ServiceSecurityModule {

	Mac mac;
	
	public BittrexSecurityModule(ServiceExchangeProperties properties) {
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
	public HttpHeaders sign(long timestamp, Method method, String requestPath, String body) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apisign", new String(Hex.encode(mac.doFinal((properties.getTradingApi() + requestPath).getBytes()))));
		return headers;
	}

	@Override
	public String appendApiRequestPathEntries(String requestPath) {
		StringBuffer sb = new StringBuffer(requestPath);
		if (!requestPath.contains("?"))
			sb.append("?");
		else
			sb.append("&");
		sb.append("apiKey=").append(apiKey);
		sb.append("&nonce=").append(secondsNonce.incrementAndGet());
		return sb.toString();
	}

	@Override
	public void appendApiPostParameterEntries(Map<String, String> postParameters) {
	}

}
