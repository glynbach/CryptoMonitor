package com.kieral.cryptomon.streaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: make the out of sequence error call back the topic as well so that only topic can be resubscribed
// means adding topic to payload as well as currency key

public class OrderedStreamingEmitter {

	private final static AtomicInteger COUNTER = new AtomicInteger(0);
	private final static SequenceComparator SEQ_COMPARATOR = new SequenceComparator();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final static long MAX_WAIT_ON_MISSING_SEQ = 1000;
	
	private final IOrderedStreamingListener listener;
	private final ExecutorService executor;

	private ConcurrentMap<String, PayloadPark> payloads = new ConcurrentHashMap<String, PayloadPark>();
	
	public OrderedStreamingEmitter(IOrderedStreamingListener listener) {
		this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "OrderedStreamingEmitter-" + COUNTER.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			}});
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(new WardenTask(), 100, 100, TimeUnit.MILLISECONDS);
		this.listener = listener;
	}
	
	public void onStreamingUpdate(StreamingPayload streamingPayload) {
		if (streamingPayload == null)
			return;
		if (streamingPayload.getCurrencyPair() == null) {
			if (logger.isTraceEnabled())
				logger.trace("Emitting payload without currency pair synchonously");
			emit(streamingPayload);
		}
		payloads.putIfAbsent(streamingPayload.getCurrencyPair(), new PayloadPark());
		PayloadPark park = payloads.get(streamingPayload.getCurrencyPair());
		// case for first in on just initialised
		park.lastSequence.compareAndSet(-100, streamingPayload.getSequenceNumber() - 1);
		if (park.lastSequence.compareAndSet(streamingPayload.getSequenceNumber() - 1, streamingPayload.getSequenceNumber())) {
			if (logger.isTraceEnabled())
				logger.trace("Emitting expected payload with sequence number " + streamingPayload.getSequenceNumber());
			executor.submit(new EmitTask(streamingPayload));
			if (park.parkedPayloads.size() > 0)
				park.review();
		} else {
			if (logger.isTraceEnabled())
				logger.trace("Parking unexpected payload with sequence number " + streamingPayload.getSequenceNumber());
			executor.submit(new ParkTask(park, streamingPayload));
		}
	}
	
	private void emit(StreamingPayload streamingPayload) {
		if (listener != null)
			listener.onOrderedStreamingPayload(streamingPayload);
	}

	private void error(String reason) {
		logger.error("Error detected in streaming sequences: " + reason);
		if (listener != null)
			listener.onError(reason);
	}

	private class EmitTask implements Runnable {

		private final StreamingPayload streamingPayload;
		private EmitTask(StreamingPayload streamingPayload) {
			this.streamingPayload = streamingPayload;
		}
		@Override
		public void run() {
			emit(streamingPayload);
		}
		
	}

	private class ErrorTask implements Runnable {

		private final String reason;
		private ErrorTask(String reason) {
			this.reason = reason;
		}
		@Override
		public void run() {
			error(reason);
		}
		
	}

	private class ParkTask implements Runnable {

		private final PayloadPark payloadPark;
		private final StreamingPayload streamingPayload;
		private ParkTask(PayloadPark payloadPark, StreamingPayload streamingPayload) {
			this.payloadPark = payloadPark;
			this.streamingPayload = streamingPayload;
		}
		@Override
		public void run() {
			payloadPark.add(streamingPayload);
			payloadPark.review();
		}
		
	}

	private class WardenTask implements Runnable {

		@Override
		public void run() {
			if (logger.isTraceEnabled())
				logger.trace("WardenTask calling for review");
			payloads.values().forEach(park -> {
				park.review();
			});
		}
		
	}
	
	private class PayloadPark {
		
		private final AtomicLong lastSequence = new AtomicLong(-100);
		private final Object parkLock = new Object();
		private final List<StreamingPayload> parkedPayloads = new ArrayList<StreamingPayload>();

		private long lastReviewSuccess = System.currentTimeMillis(); 

		private void add(StreamingPayload streamingPayload) {
			synchronized(parkLock) {
				lastReviewSuccess = System.currentTimeMillis();
				if (!parkedPayloads.contains(streamingPayload))
					parkedPayloads.add(streamingPayload);
			}
		}
		
		private void review() {
			if (parkedPayloads.size() > 0) {
				synchronized(parkLock) {
					Collections.sort(parkedPayloads, SEQ_COMPARATOR);
					if (logger.isTraceEnabled())
						logger.trace("Reviewing parkedPayloads " + parkedPayloads);
					Iterator<StreamingPayload> i = parkedPayloads.iterator();
					while (i.hasNext()) {
						StreamingPayload streamingPayload = i.next();
						if (logger.isTraceEnabled())
							logger.trace("Review comparing sequence number " + streamingPayload.getSequenceNumber() + " with last sent " + lastSequence.get());
						if (lastSequence.compareAndSet(streamingPayload.getSequenceNumber() - 1, streamingPayload.getSequenceNumber())) {
							if (logger.isTraceEnabled())
								logger.trace("Review found payload with sequence number " + streamingPayload.getSequenceNumber() + " ready for sending");
							lastReviewSuccess = System.currentTimeMillis(); 
							executor.submit(new EmitTask(streamingPayload));
							i.remove();
						} else if (lastSequence.get() >= streamingPayload.getSequenceNumber()) {
							executor.submit(new ErrorTask("Expecting sequence number " + 
									(lastSequence.get() + 1) + " but have parked message with sequence " + streamingPayload.getSequenceNumber())); 
						}
					}
					if (parkedPayloads.size() > 0 && (System.currentTimeMillis() - lastReviewSuccess) > MAX_WAIT_ON_MISSING_SEQ) {
						executor.submit(new ErrorTask("Waited " + (System.currentTimeMillis() - lastReviewSuccess) + "ms for sequence number " + 
								(lastSequence.get() + 1) + " with parked messages " + parkedPayloads)); 
					}
				}
			}
		}
	}
	
	private static class SequenceComparator implements Comparator<StreamingPayload> {

		@Override
		public int compare(StreamingPayload o1, StreamingPayload o2) {
			return Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
		}
		
	}
	
}
