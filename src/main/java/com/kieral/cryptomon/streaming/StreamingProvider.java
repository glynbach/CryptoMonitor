package com.kieral.cryptomon.streaming;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.service.connection.IStatusListener;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface StreamingProvider {

	Completable connect(StreamingProperties connectionProperties, IStatusListener listener);
	Observable<StreamingPayload> subscribe(CurrencyPair currencyPair);
	Completable disconnect();
	
}
