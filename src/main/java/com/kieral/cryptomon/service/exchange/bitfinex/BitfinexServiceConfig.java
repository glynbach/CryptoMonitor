package com.kieral.cryptomon.service.exchange.bitfinex;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.kieral.cryptomon.model.trading.TradeAmount;
import com.kieral.cryptomon.model.trading.TradingFeeType;
import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.service.exchange.ExchangeApiRequest;
import com.kieral.cryptomon.service.exchange.ServiceExchangeProperties;

@Component
@PropertySource(value = { "classpath:application.yaml" })
@ConfigurationProperties(prefix="BITFINEX")
public class BitfinexServiceConfig extends ServiceExchangeProperties {

	public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");;

	private static final String SNAPSHOT_QUERY = "/book/%s?limit_bids=20&limit_asks=20&grouped=1";
	private static final String ACCOUNTS_QUERY = "/balances";
//	private static final String OPEN_ORDERS_QUERY = "";
//	private static final String ORDER_HISTORY_QUERY = "";
//	private static final String PLACE_ORDER_QUERY = "";
//	private static final String CANCEL_ORDER_QUERY = "";
//	private static final String ORDER_QUERY = "";
//	private static final String BUY_ORDER = "";
//	private static final String SELL_ORDER = "";

	@Override
	protected String[] splitPair(String topicStr) {
		// These come as LTCBTC so need to be split by length
		String[] pairs = new String[]{topicStr.substring(0, 3), topicStr.substring(3)};
		if (pairs.length != 2)
			throw new IllegalArgumentException(String.format("Invalid currency pair symbol %s expected format "
					+ "XXXYYY", topicStr));
		return new String[]{pairs[0], pairs[1]};
	}
	
	@Override
	public ExchangeApiRequest getOrderBookSnapshotQuery(String currencyPairSymbol) {
		return new ExchangeApiRequest(snapshotApi, String.format(SNAPSHOT_QUERY, currencyPairSymbol, maxLevels), HttpMethod.GET);
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
		return null;
	}

	@Override
	public ExchangeApiRequest getCancelOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return null;
	}

	@Override
	public ExchangeApiRequest getOpenOrdersQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return null;
	}

	@Override
	public ExchangeApiRequest getOrderHistoryQuery(CurrencyPair currencyPair) {
		if (currencyPair == null || currencyPair.getTopic() == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		return null;
	}

	@Override
	public ExchangeApiRequest getOrderQuery(String orderId) {
		if (orderId == null)
			throw new IllegalArgumentException("orderId can not be null");
		return null;
	}

	@Override
	public boolean isHasGranularTrades() {
		return true;
	}

}
