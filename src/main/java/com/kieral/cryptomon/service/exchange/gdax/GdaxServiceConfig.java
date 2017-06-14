package com.kieral.cryptomon.service.exchange.gdax;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.accounting.TradingFeeType;
import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="GDAX")
public class GdaxServiceConfig extends ServiceExchangeProperties {

	private static final String SNAPSHOT_QUERY = "/products/%s/book?level=2";
	private static final String ACCOUNTS_QUERY = "/accounts";

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
	public ApiRequest getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return new ApiRequest(snapshotApi, String.format(SNAPSHOT_QUERY, currencyPairSymbol), Method.GET);
	}

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

	@Override
	public ApiRequest getAccountsQuery() {
		return new ApiRequest(snapshotApi, ACCOUNTS_QUERY, Method.GET);
	}

}
