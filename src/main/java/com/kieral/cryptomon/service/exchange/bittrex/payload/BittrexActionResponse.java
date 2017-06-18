package com.kieral.cryptomon.service.exchange.bittrex.payload;

public class BittrexActionResponse {

	private String uuid;

	public BittrexActionResponse() {
		super();
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "BittrexActionResponse [uuid=" + uuid + "]";
	}
	
}
