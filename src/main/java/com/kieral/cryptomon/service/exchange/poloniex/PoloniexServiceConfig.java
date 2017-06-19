package com.kieral.cryptomon.service.exchange.poloniex;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.general.ApiRequest;
import com.kieral.cryptomon.model.general.ApiRequest.BodyType;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="POLONIEX")
public class PoloniexServiceConfig extends ServiceExchangeProperties {

	public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String SNAPSHOT_QUERY = "?command=returnOrderBook&currencyPair=%s&depth=%s";
	private static final String ACCOUNTS_QUERY = "returnCompleteBalances";
	private static final String BUY_QUERY = "buy";
	private static final String SELL_QUERY = "sell";
	private static final String CANCEL_ORDER_QUERY = "cancelOrder";
	private static final String OPEN_ORDERS_QUERY = "returnOpenOrders";
	private static final String ORDER_HISTORY_QUERY = "returnTradeHistory";
	private static final String ORDER_QUERY = "returnOrderTrades";

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
	public ApiRequest getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return new ApiRequest(snapshotApi, String.format(SNAPSHOT_QUERY, currencyPairSymbol, maxLevels), HttpMethod.GET);
	}

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

	@Override
	public ApiRequest getAccountsQuery() {
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		apiRequest.addPostParameter("command", ACCOUNTS_QUERY);
		return apiRequest;
	}

	@Override
	public ApiRequest getPlaceOrderQuery(Side side, CurrencyPair currencyPair, BigDecimal price, TradeAmount amount) {
		if (side == null)
			throw new IllegalArgumentException("side can not be null");
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid price " + price);
		// Poloniex amounts are in base currency
		if (amount == null || amount.getBaseAmount() == null || amount.getBaseAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid amount " + amount);
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		if (side == Side.BID)
			apiRequest.addPostParameter("command", BUY_QUERY);
		else
			apiRequest.addPostParameter("command", SELL_QUERY);
		apiRequest.addPostParameter("currencyPair", currencyPair.getTopic());
		apiRequest.addPostParameter("rate", price.stripTrailingZeros().toPlainString());
		apiRequest.addPostParameter("amount", amount.getBaseAmount().stripTrailingZeros().toPlainString());
		return apiRequest;
	}

	@Override
	public ApiRequest getCancelOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		apiRequest.addPostParameter("command", CANCEL_ORDER_QUERY);
		apiRequest.addPostParameter("orderNumber", orderId);
		return apiRequest;
	}

	@Override
	public ApiRequest getOpenOrdersQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		apiRequest.addPostParameter("command", OPEN_ORDERS_QUERY);
		apiRequest.addPostParameter("currencyPair", currencyPair.getTopic());
		return apiRequest;
	}

	@Override
	public ApiRequest getOrderHistoryQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		apiRequest.addPostParameter("command", ORDER_HISTORY_QUERY);
		apiRequest.addPostParameter("currencyPair", currencyPair.getTopic());
		return apiRequest;
	}

	@Override
	public ApiRequest getOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		ApiRequest apiRequest = new ApiRequest(tradingApi, "", HttpMethod.POST, BodyType.URLENCODED);
		apiRequest.addPostParameter("command", ORDER_QUERY);
		apiRequest.addPostParameter("orderNumber", orderId);
		return apiRequest;
	}

}
