package com.kieral.cryptomon.service.exchange.bittrex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.accounting.TradingFeeType;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="BITTREX")
public class BittrexServiceConfig extends ServiceExchangeProperties {

	private static final String SNAPSHOT_QUERY = "/getorderbook?market=%s&type=both&depth=%s";

	@Override
	protected String[] splitPair(String topicStr) {
		// These come as BTC-LTC so need to be reversed as LTC is the base currency and BTC is the quoted currency
		String[] pairs = topicStr.split("-");
		if (pairs.length != 2)
			throw new IllegalArgumentException(String.format("Invalid currency pair symbol %s expected format "
					+ "XXX-YYY", topicStr));
		return new String[]{pairs[1], pairs[0]};
	}
	
	@Override
	public String getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return snapshotApi + String.format(SNAPSHOT_QUERY, currencyPairSymbol, maxLevels);
	}

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

}
