package com.kieral.cryptomon.service.exchange.gdax;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.exchange.ExchangeApiRequest;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;
import com.kieral.cryptomon.service.exchange.ExchangeApiRequest.BodyType;
import com.kieral.cryptomon.service.exchange.ExchangeApiRequest.ResponseErrorAction;
import com.kieral.cryptomon.service.exchange.ExchangeApiRequest.ResponseErrorChecker;

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

	// Response when cancelling order that can not be found
	private final Function<String, ResponseErrorAction> messageNotFoundFunction = new Function<String, ResponseErrorAction>() {
    	public ResponseErrorAction apply(String response) {
			if (response != null && response.contains("\"message\"") && response.contains("Not") && response.contains("Found"))
				return ResponseErrorAction.CANCEL;
			return ResponseErrorAction.RAISE_EXCEPTION;
    	}
    };

	// Response when cancelling order that has already been completed
	private final Function<String, ResponseErrorAction> messageAlreadyDoneFunction = new Function<String, ResponseErrorAction>() {
    	public ResponseErrorAction apply(String response) {
			if (response != null && response.contains("\"message\"") && response.contains("already done"))
				return ResponseErrorAction.USE_PREVIOUS;
			return ResponseErrorAction.RAISE_EXCEPTION;
    	}
    };

    // Multiple messages available on placing an invalid order
	private final Function<String, ResponseErrorAction> messageExistsFunction = new Function<String, ResponseErrorAction>() {
    	public ResponseErrorAction apply(String response) {
			if (response != null && response.contains("\"message\"") && response.contains(":"))
				return ResponseErrorAction.CANCEL;
			return ResponseErrorAction.RAISE_EXCEPTION;
    	}
    };
	private final ResponseErrorChecker notFoundStatus = new ResponseErrorChecker(HttpStatus.NOT_FOUND, messageNotFoundFunction);
	private final ResponseErrorChecker alreadyDoneStatus = new ResponseErrorChecker(HttpStatus.BAD_REQUEST, messageAlreadyDoneFunction);
	private final ResponseErrorChecker generalErrorStatus = new ResponseErrorChecker(HttpStatus.BAD_REQUEST, messageExistsFunction);

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
	public ExchangeApiRequest getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return new ExchangeApiRequest(snapshotApi, String.format(SNAPSHOT_QUERY, currencyPairSymbol), HttpMethod.GET);
	}

	@Override
	public TradingFeeType getTradingFeeType() {
		return TradingFeeType.PERCENTAGE;
	}

	@Override
	public ExchangeApiRequest getAccountsQuery() {
		return new ExchangeApiRequest(tradingApi, ACCOUNTS_QUERY, HttpMethod.GET);
	}

	@Override
	public ExchangeApiRequest getPlaceOrderQuery(Side side, CurrencyPair currencyPair, BigDecimal price, TradeAmount amount) {
		if (side == null)
			throw new IllegalArgumentException("side can not be null");
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid price " + price);
		// Gdax amounts are in base currency
		if (amount == null || amount.getBaseAmount() == null || amount.getBaseAmount().compareTo(BigDecimal.ZERO) <= 0)
			throw new IllegalArgumentException("invalid amount " + amount);
		ExchangeApiRequest apiRequest = new ExchangeApiRequest(tradingApi, PLACE_ORDER_QUERY, HttpMethod.POST, BodyType.JSON, generalErrorStatus);
		apiRequest.addPostParameter("side", side == Side.BID ? "buy" : "sell");
		apiRequest.addPostParameter("product_id", currencyPair.getTopic());
		apiRequest.addPostParameter("type", "limit");
		apiRequest.addPostParameter("size", amount.getBaseAmount().stripTrailingZeros().toPlainString());
		apiRequest.addPostParameter("price", price.stripTrailingZeros().toPlainString());
		apiRequest.addPostParameter("time_in_force", "GTC");
		return apiRequest;
	}

	@Override
	public ExchangeApiRequest getCancelOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ExchangeApiRequest(tradingApi, String.format(CANCEL_ORDER_QUERY, orderId), HttpMethod.DELETE, BodyType.JSON, notFoundStatus, alreadyDoneStatus);
	}

	@Override
	public ExchangeApiRequest getOpenOrdersQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ExchangeApiRequest(tradingApi, String.format(OPEN_ORDERS_QUERY, currencyPair.getTopic()), HttpMethod.GET);
	}

	@Override
	public ExchangeApiRequest getOrderHistoryQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return new ExchangeApiRequest(tradingApi, String.format(ORDER_HISTORY_QUERY, currencyPair.getTopic()), HttpMethod.GET);
	}

	@Override
	public ExchangeApiRequest getOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return new ExchangeApiRequest(tradingApi, String.format(ORDER_QUERY, orderId), HttpMethod.GET, BodyType.JSON, notFoundStatus);
	}

	@Override
	public boolean isHasGranularTrades() {
		return true;
	}
	
}
