package com.kieral.cryptomon.streaming;

public interface OrderedStreamingListener {

	void onOrderedStreamingPayload(StreamingPayload streamingPayload);
	void onOrderedStreamingError(String topic, String reason);
	
}
