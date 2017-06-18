package com.kieral.cryptomon.service.exchange.gdax;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.general.ApiRequest.BodyType;
import com.kieral.cryptomon.model.general.ApiRequest.Method;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="GDAX")
public class GdaxServiceConfig extends ServiceExchangeProperties {
	
	public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSX");;
	
	private static final String SNAPSHOT_QUERY = "/products/%s/book?level=2";
	private static final String ACCOUNTS_QUERY = "/accounts";
	private static final String ORDER_QUERY = "/orders/%s";
	private static final String PLACE_ORDER_QUERY = "/orders";
	private static final String CANCEL_ORDER_QUERY = "/orders/%s";
	private static final String OPEN_ORDERS_QUERY = "/orders?status=all&product_id=%s";
	private static final String ORDER_HISTORY_QUERY = "/fills?product_id=%s";

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
		return new ApiRequest(tradingApi, ACCOUNTS_QUERY, Method.GET);
	}

	@Override
	public ApiRequest getPlaceOrderQuery(Side side, CurrencyPair currencyPair, BigDecimal price, TradeAmount amount) {
		if (side == null)
			throw new IllegalArgumentException("side can not be null");
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid price " + price);
		// Gdax amounts are in base currency
		if (amount == null || amount.getBaseAmount() == null || amount.getBaseAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid amount " + amount);
		ApiRequest apiRequest = new ApiRequest(tradingApi, PLACE_ORDER_QUERY, Method.POST, BodyType.JSON);
		apiRequest.addPostParameter("side", side == Side.BID ? "buy" : "sell");
		apiRequest.addPostParameter("product_id", currencyPair.getTopic());
		apiRequest.addPostParameter("type", "limit");
		apiRequest.addPostParameter("size", amount.getBaseAmount().stripTrailingZeros().toPlainString());
		apiRequest.addPostParameter("price", price.stripTrailingZeros().toPlainString());
		apiRequest.addPostParameter("time_in_force", "GTC");
		return apiRequest;
	}

	@Override
	public ApiRequest getCancelOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ApiRequest(tradingApi, String.format(CANCEL_ORDER_QUERY, orderId), Method.DELETE);
	}

	@Override
	public ApiRequest getOpenOrdersQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ApiRequest(tradingApi, String.format(OPEN_ORDERS_QUERY, currencyPair.getTopic()), Method.GET);
	}

	@Override
	public ApiRequest getOrderHistoryQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ApiRequest(tradingApi, String.format(ORDER_HISTORY_QUERY, currencyPair.getTopic()), Method.GET);
	}

	@Override
	public ApiRequest getOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ApiRequest(tradingApi, String.format(ORDER_QUERY, orderId), Method.GET);
	}

}
