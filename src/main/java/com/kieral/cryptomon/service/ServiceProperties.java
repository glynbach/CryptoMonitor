package com.kieral.cryptomon.service;

import java.util.List;

import com.kieral.cryptomon.service.liquidity.SubscriptionProperties;

public class ServiceProperties {

	private String uri;
	private String apiKey;
	private int transactionsPerSecond;
	private List<SubscriptionProperties> marketDataTopics;
	private boolean sipValidationOnEmptyPayloads;
	private boolean requiresSnapshot;
	private boolean useSnapshotSequence;

	public static class Builder {
		private String uri;
		private String apiKey;
		private int transactionsPerSecond;
		private List<SubscriptionProperties> marketDataTopics;
		private boolean sipValidationOnEmptyPayloads;
		private boolean requiresSnapshot;
		private boolean useSnapshotSequence;

		public Builder uri(String uri) {
			this.uri = uri;
			return this;
		}

		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder transactionsPerSecond(int transactionsPerSecond) {
			this.transactionsPerSecond = transactionsPerSecond;
			return this;
		}

		public Builder marketDataTopics(List<SubscriptionProperties> marketDataTopics) {
			this.marketDataTopics = marketDataTopics;
			return this;
		}

		public Builder sipValidationOnEmptyPayloads(boolean sipValidationOnEmptyPayloads) {
			this.sipValidationOnEmptyPayloads = sipValidationOnEmptyPayloads;
			return this;
		}

		public Builder requiresSnapshot(boolean requiresSnapshot) {
			this.requiresSnapshot = requiresSnapshot;
			return this;
		}

		public Builder useSnapshotSequence(boolean useSnapshotSequence) {
			this.useSnapshotSequence = useSnapshotSequence;
			return this;
		}

		public ServiceProperties build() {
			return new ServiceProperties(this);
		}
	}

	private ServiceProperties(Builder builder) {
		this.uri = builder.uri;
		this.apiKey = builder.apiKey;
		this.transactionsPerSecond = builder.transactionsPerSecond;
		this.marketDataTopics = builder.marketDataTopics;
		this.sipValidationOnEmptyPayloads = builder.sipValidationOnEmptyPayloads;
		this.requiresSnapshot = builder.requiresSnapshot;
		this.useSnapshotSequence = builder.useSnapshotSequence;
	}

	public String getUri() {
		return uri;
	}

	public String getApiKey() {
		return apiKey;
	}

	public int getTransactionsPerSecond() {
		return transactionsPerSecond;
	}

	public List<SubscriptionProperties> getMarketDataTopics() {
		return marketDataTopics;
	}

	public boolean isSipValidationOnEmptyPayloads() {
		return sipValidationOnEmptyPayloads;
	}

	public boolean isRequiresSnapshot() {
		return requiresSnapshot;
	}

	public boolean isUseSnapshotSequence() {
		return useSnapshotSequence;
	}

	@Override
	public String toString() {
		return "ServiceProperties [uri=" + uri + ", apiKey=" + apiKey + ", transactionsPerSecond="
				+ transactionsPerSecond + ", marketDataTopics=" + marketDataTopics + ", sipValidationOnEmptyPayloads="
				+ sipValidationOnEmptyPayloads + ", requiresSnapshot=" + requiresSnapshot + ", useSnapshotSequence="
				+ useSnapshotSequence + "]";
	}
	
}
