package com.kieral.cryptomon.streaming;

import com.kieral.cryptomon.service.connection.IStatusListener;

import io.reactivex.Completable;
import io.reactivex.Observable;

public interface StreamingProvider {

	Completable connect(StreamingProperties connectionProperties, IStatusListener listener);
	Observable<StreamingPayload> subscribe(String topic, String currencyPair);
	Completable disconnect();
	
}
