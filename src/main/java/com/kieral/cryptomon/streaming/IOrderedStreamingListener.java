package com.kieral.cryptomon.streaming;

public interface IOrderedStreamingListener {

	void onOrderedStreamingPayload(StreamingPayload streamingPayload);
	void onOrderedStreamingError(String topic, String reason);
	
}
