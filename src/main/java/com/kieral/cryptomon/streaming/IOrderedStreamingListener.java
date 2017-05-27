package com.kieral.cryptomon.streaming;

public interface IOrderedStreamingListener {

	void onOrderedStreamingPayload(StreamingPayload streamingPayload);
	void onError(String reason);
	
}
