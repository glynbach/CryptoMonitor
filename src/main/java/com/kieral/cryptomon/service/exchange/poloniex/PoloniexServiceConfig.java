package com.kieral.cryptomon.service.exchange.poloniex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.ServiceProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="POLONIEX")
public class PoloniexServiceConfig extends ServiceProperties {

	private static final String SNAPSHOT_QUERY = "?command=returnOrderBook&currencyPair=%s&depth=20";

	@Override
	protected String[] splitPair(String topicStr) {
		String[] pairs = topicStr.split("_");
		if (pairs.length != 2)
			throw new IllegalArgumentException(String.format("Invalid currency pair symbol %s expected format "
					+ "XXX_YYY", topicStr));
		return pairs;
	}
	
	@Override
	public String getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return snapshotApi + String.format(SNAPSHOT_QUERY, currencyPairSymbol);
	}

}
