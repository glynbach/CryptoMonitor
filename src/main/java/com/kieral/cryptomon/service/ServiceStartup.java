package com.kieral.cryptomon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.tickstore.TickstoreService;
import com.kieral.cryptomon.service.util.LoggingUtils;

@Component
public class ServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired 
	private TickstoreService tickstoreService;

	@Autowired 
	ExchangeManagerService exchangeManager;

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
		logger.info("Received application ready event - starting services");
		if (LoggingUtils.isTickstoreLoggingEnabled()) {
			logger.info("Starting tickstore");
			tickstoreService.init();
		}
		exchangeManager.getEnabledExchanges().forEach(exchange -> {
			logger.info("Starting {}", exchange.getName());
			try {
				exchange.connect();
			} catch (Exception e) {
				logger.error("Error starting {}", exchange.getName(), e);
			}
		});
		return;
	}
}