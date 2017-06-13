package com.kieral.cryptomon.service.tickstore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.tickstore.DaoManager;

@Component
public class TickstoreService {

	@Autowired
	DaoManager daoManager;
	
	@Autowired
	ExchangeManagerService exchangeManager;
	
	public TickstoreService() {
	}
	
	public void init() {
		if (LoggingUtils.isTickstoreLoggingEnabled()) {
			exchangeManager.getEnabledExchanges().forEach(service -> {
				service.registerOrderBookListener(orderBook -> {
					daoManager.saveOrderBookDataEntries(orderBook);
				});
			});
		}
	}

}
