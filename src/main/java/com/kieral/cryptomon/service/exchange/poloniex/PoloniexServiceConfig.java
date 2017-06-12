package com.kieral.cryptomon.service.exchange.poloniex;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.accounting.TradingFeeType;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="POLONIEX")
public class PoloniexServiceConfig extends ServiceExchangeProperties {

	private static final String SNAPSHOT_QUERY = "?command=returnOrderBook&currencyPair=%s&depth=%s";

	@Override
	protected String[] splitPair(String topicStr) {
		// These come as BTC_LTC so need to be reversed as LTC is the base currency and BTC is the quoted currency
		String[] pairs = topicStr.split("_");
		if (pairs.length != 2)
			throw new IllegalArgumentException(String.format("Invalid currency pair symbol %s expected format "
					+ "XXX_YYY", topicStr));
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
