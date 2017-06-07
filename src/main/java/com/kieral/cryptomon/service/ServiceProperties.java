package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.kieral.cryptomon.model.Currency;
import com.kieral.cryptomon.model.CurrencyPair;

public abstract class ServiceProperties {

	public enum SubscriptionMode {
		POLLING,
		STREAMING
	}
	
    protected String exchange;
    protected String pushApi;
    protected String snapshotApi;
    protected String tradingApi;
    protected SubscriptionMode subscriptionMode;
    protected long pollingInterval;
    protected int maxTransPerSecond;
    protected boolean skipHearbeats;
    protected boolean snapshotBaseline;
    protected boolean snapshotBaselineSequence;
    protected String apiKeyLoc;
    protected String apiSecretLoc;
    protected List<CurrencyPairProperties> currencyPairs;

	private final AtomicBoolean initialised = new AtomicBoolean(false);
	private final List<CurrencyPair> pairs = new ArrayList<CurrencyPair>();

    public ServiceProperties() {
    }
    
	public ServiceProperties(String exchange, String pushApi, String snapshotApi, String tradingApi,
			SubscriptionMode subscriptionMode, long pollingInterval, int maxTransPerSecond, boolean skipHearbeats, boolean snapshotBaseline, 
			boolean snapshotBaselineSequence, String apiKeyLoc, String apiSecretLoc, List<CurrencyPairProperties> currencyPairs) {
		super();
		this.exchange = exchange;
		this.pushApi = pushApi;
		this.snapshotApi = snapshotApi;
		this.tradingApi = tradingApi;
		this.subscriptionMode = subscriptionMode;
		this.pollingInterval = pollingInterval;
		this.maxTransPerSecond = maxTransPerSecond;
		this.skipHearbeats = skipHearbeats;
		this.snapshotBaseline = snapshotBaseline;
		this.snapshotBaselineSequence = snapshotBaselineSequence;
		this.apiKeyLoc = apiKeyLoc;
		this.apiSecretLoc = apiSecretLoc;
		this.currencyPairs = currencyPairs;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getPushApi() {
		return pushApi;
	}

	public void setPushApi(String pushApi) {
		this.pushApi = pushApi;
	}

	public String getSnapshotApi() {
		return snapshotApi;
	}

	public void setSnapshotApi(String snapshotApi) {
		this.snapshotApi = snapshotApi;
	}

	public String getTradingApi() {
		return tradingApi;
	}

	public void setTradingApi(String tradingApi) {
		this.tradingApi = tradingApi;
	}

	public SubscriptionMode getSubscriptionMode() {
		return subscriptionMode;
	}

	public void setSubscriptionMode(SubscriptionMode subscriptionMode) {
		this.subscriptionMode = subscriptionMode;
	}

	public long getPollingInterval() {
		return pollingInterval;
	}

	public void setPollingInterval(long pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	public int getMaxTransPerSecond() {
		return maxTransPerSecond;
	}

	public void setMaxTransPerSecond(int maxTransPerSecond) {
		this.maxTransPerSecond = maxTransPerSecond;
	}

	public boolean isSkipHearbeats() {
		return skipHearbeats;
	}

	public void setSkipHearbeats(boolean skipHearbeats) {
		this.skipHearbeats = skipHearbeats;
	}

	public boolean isSnapshotBaseline() {
		return snapshotBaseline;
	}

	public void setSnapshotBaseline(boolean snapshotBaseline) {
		this.snapshotBaseline = snapshotBaseline;
	}

	public boolean isSnapshotBaselineSequence() {
		return snapshotBaselineSequence;
	}

	public void setSnapshotBaselineSequence(boolean snapshotBaselineSequence) {
		this.snapshotBaselineSequence = snapshotBaselineSequence;
	}

	public String getApiKeyLoc() {
		return apiKeyLoc;
	}

	public void setApiKeyLoc(String apiKeyLoc) {
		this.apiKeyLoc = apiKeyLoc;
	}

	public String getApiSecretLoc() {
		return apiSecretLoc;
	}

	public void setApiSecretLoc(String apiSecretLoc) {
		this.apiSecretLoc = apiSecretLoc;
	}

	public List<CurrencyPairProperties> getCurrencyPairs() {
		return currencyPairs;
	}

	public void setCurrencyPairs(List<CurrencyPairProperties> currencyPairs) {
		this.currencyPairs = currencyPairs;
	}

	protected abstract String[] splitPair(String topicStr);
	
	public CurrencyPair getPair(CurrencyPairProperties currencyPair) {
		if (currencyPair == null)
			throw new IllegalArgumentException("currencyPair can not be null");
		String topic = currencyPair.getPair();
		String[] pairs = splitPair(currencyPair.getPair());
		Currency currency1 = Currency.valueOf(pairs[0].trim());
		Currency currency2 = Currency.valueOf(pairs[1].trim());
		// TODO: add trading fees and currency
		return new CurrencyPair(pairs[0].trim() + pairs[1].trim(), currency1, currency2, topic.trim(),
				currencyPair.getPriceScale(), currencyPair.getTradingFee() != null ? new BigDecimal(currencyPair.getTradingFee()) : BigDecimal.ZERO, null);
	}

	public List<CurrencyPair> getPairs() {
		if (currencyPairs == null)
			return null;
		if (initialised.compareAndSet(false, true)) {
			currencyPairs.forEach(pair -> {
				pairs.add(getPair(pair));
			});
		}
		return pairs;
	}

	public abstract String getOrderBookSnapshotQuery(String currencyPairSymbol);

	@Override
	public String toString() {
		return "ServiceProperties [exchange=" + exchange + ", pushApi=" + pushApi + ", snapshotApi=" + snapshotApi
				+ ", tradingApi=" + tradingApi + ", subscriptionMode=" + subscriptionMode + ", pollingInterval="
				+ pollingInterval + ", maxTransPerSecond=" + maxTransPerSecond + ", skipHearbeats=" + skipHearbeats
				+ ", snapshotBaseline=" + snapshotBaseline + ", snapshotBaselineSequence=" + snapshotBaselineSequence
				+ ", apiKeyLoc=" + apiKeyLoc + ", apiSecretLoc=" + apiSecretLoc + ", currencyPairs=" + currencyPairs
				+ "]";
	}
	
}
