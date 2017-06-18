package com.kieral.cryptomon.service.exchange;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exception.SecurityModuleException;
import com.kieral.cryptomon.service.util.EncryptionUtil;

public abstract class ServiceSecurityModule {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final AtomicBoolean tradingEnabled = new AtomicBoolean(false);
	protected static final AtomicLong secondsNonce = new AtomicLong(Instant.now().getEpochSecond());
	protected static final AtomicLong millisNonce = new AtomicLong(System.currentTimeMillis());
	
	protected final ServiceExchangeProperties properties;
	
	protected String apiKey;
	protected String apiSecret;
	protected String apiPassphrase;
	
	public ServiceSecurityModule(ServiceExchangeProperties properties) {
		this.properties = properties;
	}

	public boolean enable(String secretKey) {
		if (tradingEnabled.compareAndSet(false, true)) {
			boolean enabled = true;
			if (properties.getApiKeyLoc() != null ) {
				try {
					apiKey = EncryptionUtil.decryptValue(secretKey, new File(properties.getApiKeyLoc()));
				} catch (Exception e) {
					enabled = false;
					logger.error("Error reading apiKey from {}", properties.getApiKeyLoc(), e);
				}
			}
			if (properties.getApiSecretLoc() != null ) {
				try {
					apiSecret = EncryptionUtil.decryptValue(secretKey, new File(properties.getApiSecretLoc()));
				} catch (Exception e) {
					enabled = false;
					logger.error("Error reading apiSecret from {}", properties.getApiKeyLoc(), e);
				}
			}
			if (properties.getApiPassphraseLoc() != null ) {
				try {
					apiPassphrase = EncryptionUtil.decryptValue(secretKey, new File(properties.getApiPassphraseLoc()));
				} catch (Exception e) {
					enabled = false;
					logger.error("Error reading apiPassphrase from {}", properties.getApiPassphraseLoc(), e);
				}
			}
			if (enabled) {
				try {
					initialise();
				} catch (Exception e) {
					logger.error("Error initialising security module", e);
					enabled = false;
				}
			}
			tradingEnabled.set(enabled);
		}
		return tradingEnabled.get();
	}
	
	public boolean isEnabled() {
		return tradingEnabled.get();
	}

	public abstract void initialise() throws SecurityModuleException;
	public abstract String appendApiRequestPathEntries(String requestPath);
	public abstract void appendApiPostParameterEntries(Map<String, String> postParameters);
	/**
	 * @param timestamp that will be sent in the message
	 * @param method GET or POST
	 * @param requestPath of the URI being requested
	 * @param body null if the method is GET
	 * @return
	 */
	public abstract HttpHeaders sign(long timestamp, Method method, String requestPath, String body) throws SecurityModuleException;
	
}
