package com.kieral.cryptomon.streaming;

public class StreamingProperties {

	private final String uri;
	private final String apiKey;
	private final String realm;

	public static class Builder {
		
		private String uri;
		private String apiKey;
		private String realm;

		public Builder uri(String uri) {
			this.uri = uri;
			return this;
		}

		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder realm(String realm) {
			this.realm = realm;
			return this;
		}

		public StreamingProperties build() {
			return new StreamingProperties(this);
		}
	}

	public String getUri() {
		return uri;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getRealm() {
		return realm;
	}

	private StreamingProperties(Builder builder) {
		this.uri = builder.uri;
		this.apiKey = builder.apiKey;
		this.realm = builder.realm;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((apiKey == null) ? 0 : apiKey.hashCode());
		result = prime * result + ((realm == null) ? 0 : realm.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		StreamingProperties other = (StreamingProperties) obj;
		if (apiKey == null) {
			if (other.apiKey != null)
				return false;
		} else if (!apiKey.equals(other.apiKey))
			return false;
		if (realm == null) {
			if (other.realm != null)
				return false;
		} else if (!realm.equals(other.realm))
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StreamingProperties [uri=" + uri + ", apiKey=" + apiKey + ", realm=" + realm + "]";
	}
	
}
