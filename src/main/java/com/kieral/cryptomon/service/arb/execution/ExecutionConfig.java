package com.kieral.cryptomon.service.arb.execution;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="execution")
public class ExecutionConfig {

	BigDecimal minValue;
	long pollingInterval;
	
	public BigDecimal getMinValue() {
		return minValue;
	}
	
	public void setMinValue(BigDecimal minValue) {
		this.minValue = minValue;
	}
	
	public long getPollingInterval() {
		return pollingInterval;
	}
	
	public void setPollingInterval(long pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	@Override
	public String toString() {
		return "ExecutionConfig [minValue=" + minValue + ", pollingInterval=" + pollingInterval + "]";
	}
	
}
