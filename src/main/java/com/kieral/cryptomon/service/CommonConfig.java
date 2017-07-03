package com.kieral.cryptomon.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="common")
public class CommonConfig {

	long pollingInterval;
	
	public long getPollingInterval() {
		return pollingInterval;
	}
	
	public void setPollingInterval(long pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	@Override
	public String toString() {
		return "CommonConfig [pollingInterval=" + pollingInterval + "]";
	}
	
}
