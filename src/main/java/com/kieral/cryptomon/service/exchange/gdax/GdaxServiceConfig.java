package com.kieral.cryptomon.service.exchange.gdax;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.service.ServiceProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="GDAX")
public class GdaxServiceConfig extends ServiceProperties {

	private static final String SNAPSHOT_QUERY = "/products/%s/book?level=2";

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
