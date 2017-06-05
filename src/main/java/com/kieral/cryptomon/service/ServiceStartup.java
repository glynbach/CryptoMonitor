package com.kieral.cryptomon.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.liquidity.AbstractLiquidityProvider;

@Component
public class ServiceStartup implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired 
	private AbstractLiquidityProvider poloniexService;

	@Override
	public void onApplicationEvent(final ApplicationReadyEvent event) {
    	try {
			poloniexService.connect();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return;
	}
}