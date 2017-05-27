package com.kieral.cryptomon.service;

public class SubscriptionProperties {

	private final String currencyPair;
	private final String topic;

	public static class Builder {
		private String currencyPair;
		private String topic;

		public Builder currencyPair(String currencyPair) {
			this.currencyPair = currencyPair;
			return this;
		}

		public Builder topic(String topic) {
			this.topic = topic;
			return this;
		}

		public SubscriptionProperties build() {
			return new SubscriptionProperties(this);
		}
	}

	private SubscriptionProperties(Builder builder) {
		this.currencyPair = builder.currencyPair;
		this.topic = builder.topic;
	}

	public String getCurrencyPair() {
		return currencyPair;
	}

	public String getTopic() {
		return topic;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currencyPair == null) ? 0 : currencyPair.hashCode());
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SubscriptionProperties other = (SubscriptionProperties) obj;
		if (currencyPair == null) {
			if (other.currencyPair != null)
				return false;
		} else if (!currencyPair.equals(other.currencyPair))
			return false;
		if (topic == null) {
			if (other.topic != null)
				return false;
		} else if (!topic.equals(other.topic))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubscriptionProperties [currencyPair=" + currencyPair + ", topic=" + topic + "]";
	}
	
}
