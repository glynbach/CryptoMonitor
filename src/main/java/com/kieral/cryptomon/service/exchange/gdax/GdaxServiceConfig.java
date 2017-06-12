package com.kieral.cryptomon.service.exchange.gdax;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.accounting.TradingFeeType;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="GDAX")
public class GdaxServiceConfig extends ServiceExchangeProperties {

	private static final String SNAPSHOT_QUERY = "/products/%s/book?level=2";

	@Override
	protected String[] splitPair(String topicStr) {
		// These come as LTC-BTC so is the correct way round for LTC as base currency and BTC is the quoted currency
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

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

}
