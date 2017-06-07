package com.kieral.cryptomon.service.exchange.bittrex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.ServiceProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="BITTREX")
public class BittrexServiceConfig extends ServiceProperties {

	private static final String SNAPSHOT_QUERY = "/getorderbook?market=%s&type=both&depth=20";

	@Override
	protected String[] splitPair(String topicStr) {
		String[] pairs = topicStr.split("-");
		if (pairs.length != 2)
			throw new IllegalArgumentException(String.format("Invalid currency pair symbol %s expected format "
					+ "XXX-YYY", topicStr));
		return pairs;
	}
	
	@Override
	public String getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return snapshotApi + String.format(SNAPSHOT_QUERY, currencyPairSymbol);
	}

}
