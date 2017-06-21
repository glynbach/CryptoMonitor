package com.kieral.cryptomon.service.exchange.bittrex;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="BITTREX")
public class BittrexServiceConfig extends ServiceExchangeProperties {

	public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");;

	private static final String SNAPSHOT_QUERY = "/public/getorderbook?market=%s&type=both&depth=%s";
	private static final String ACCOUNTS_QUERY = "/account/getbalances";
	private static final String OPEN_ORDERS_QUERY = "/market/getopenorders?market=%s";
	private static final String ORDER_HISTORY_QUERY = "/account/getorderhistory?market=%s";
	private static final String PLACE_ORDER_QUERY = "/market/%s?market=%s&quantity=%s&rate=%s";
	private static final String CANCEL_ORDER_QUERY = "/market/cancel?uuid=%s";
	private static final String ORDER_QUERY = "/account/getorder?uuid=%s";
	private static final String BUY_ORDER = "buylimit";
	private static final String SELL_ORDER = "selllimit";

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
	public ApiRequest getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return new ApiRequest(snapshotApi, String.format(SNAPSHOT_QUERY, currencyPairSymbol, maxLevels), HttpMethod.GET);
	}

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

	@Override
	public ApiRequest getAccountsQuery() {
		return new ApiRequest(tradingApi, ACCOUNTS_QUERY, HttpMethod.GET);
	}

	@Override
	public ApiRequest getPlaceOrderQuery(Side side, CurrencyPair currencyPair, BigDecimal price, TradeAmount amount) {
		if (side == null)
			throw new IllegalArgumentException("side can not be null");
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid price " + price);
		// Bittrex amounts are in base currency
		if (amount == null || amount.getBaseAmount() == null || amount.getBaseAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid amount " + amount);
		return new ApiRequest(tradingApi, String.format(PLACE_ORDER_QUERY, side == Side.BID ? BUY_ORDER : SELL_ORDER, 
				currencyPair.getTopic(), amount.getBaseAmount().stripTrailingZeros().toPlainString(), 
				price.stripTrailingZeros().toPlainString()), HttpMethod.GET);
	}

	@Override
	public ApiRequest getCancelOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ApiRequest(tradingApi, String.format(CANCEL_ORDER_QUERY, orderId), HttpMethod.GET);
	}

	@Override
	public ApiRequest getOpenOrdersQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ApiRequest(tradingApi, String.format(OPEN_ORDERS_QUERY, currencyPair.getTopic()), HttpMethod.GET);
	}

	@Override
	public ApiRequest getOrderHistoryQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ApiRequest(tradingApi, String.format(ORDER_HISTORY_QUERY, currencyPair.getTopic()), HttpMethod.GET);
	}

	@Override
	public ApiRequest getOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ApiRequest(tradingApi, String.format(ORDER_QUERY, orderId), HttpMethod.GET);
	}

	@Override
	public boolean isHasGranularTrades() {
		return false;
	}

}
