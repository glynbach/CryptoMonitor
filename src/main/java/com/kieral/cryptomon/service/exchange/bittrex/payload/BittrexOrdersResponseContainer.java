package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.util.Collections;
import java.util.List;

import com.kieral.cryptomon.service.rest.OrdersResponse;

public class BittrexOrdersResponseContainer extends BittrexResponseContainer<List<BittrexOrderResponse>> implements OrdersResponse<BittrexOrderResponse> {

	@Override
	public List<BittrexOrderResponse> getOrderResponses() {
		return this.getResult() == null ? Collections.emptyList() : this.getResult().subList(0, this.getResult().size());
	}

}
