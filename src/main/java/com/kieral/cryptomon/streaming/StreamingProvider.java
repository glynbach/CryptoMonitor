package com.kieral.cryptomon.streaming;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.service.connection.ConnectionStatusListener;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface StreamingProvider {

	Completable connect(StreamingProperties connectionProperties, ConnectionStatusListener listener);
	Observable<StreamingPayload> subscribe(CurrencyPair currencyPair);
	Completable disconnect();
	
}
