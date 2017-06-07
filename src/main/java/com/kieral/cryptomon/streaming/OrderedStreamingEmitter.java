package com.kieral.cryptomon.streaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.service.util.threading.StickyThreadPool;

// TODO: handle very first 2 messages are out of sequence order: e.g. 142, 141, 143

public class OrderedStreamingEmitter {

	private final static AtomicInteger COUNTER = new AtomicInteger(0);
	private final static SequenceComparator SEQ_COMPARATOR = new SequenceComparator();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final static long MAX_WAIT_ON_MISSING_SEQ = 1000;
	
	private final IOrderedStreamingListener listener;
	private final boolean snapshotRequired;
	private final boolean useSnapshotSequence;
	private final ConcurrentMap<String, AtomicBoolean> snapshotsReceived = new ConcurrentHashMap<String, AtomicBoolean>();
	private final ConcurrentMap<String, Long> snapshotSequences = new ConcurrentHashMap<String, Long>();
	
	private final StickyThreadPool stickyThreadPool;

	private ConcurrentMap<String, PayloadPark> payloads = new ConcurrentHashMap<String, PayloadPark>();
	
	public OrderedStreamingEmitter(String market, IOrderedStreamingListener listener, boolean snapshotRequired,
			boolean useSnapshotSequence, int processorPoolSize) {
		this.stickyThreadPool = new StickyThreadPool(market, processorPoolSize);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "OrderedStreamingScheduler-" + COUNTER.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			}});
		scheduler.scheduleAtFixedRate(new WardenTask(), 100, 100, TimeUnit.MILLISECONDS);
		this.listener = listener;
		this.snapshotRequired = snapshotRequired;
		this.useSnapshotSequence = useSnapshotSequence;
	}
	
	public void onStreamingUpdate(final StreamingPayload streamingPayload) {
		if (streamingPayload == null)
			return;
		if (streamingPayload.getCurrencyPair() == null) {
			if (LoggingUtils.isDataBufferingLoggingEnabled())
				logger.info("Emitting payload without currency pair synchonously");
			emit(streamingPayload);
		}
		payloads.putIfAbsent(streamingPayload.getCurrencyPair().getName(), new PayloadPark(streamingPayload.getCurrencyPair()));
		stickyThreadPool.getSingleThreadExecutor(streamingPayload.getCurrencyPair().getName())
			.submit(() -> {
				try {
					PayloadPark park = payloads.get(streamingPayload.getCurrencyPair().getName());
					park.onStreamingPayload(streamingPayload);
				} catch (Exception e) {
					logger.error("Exception on StreamingUpdate for {}", streamingPayload, e);
					error(streamingPayload.getCurrencyPair().getTopic(), e.getMessage());
				}
			});
	}

	public void onSnashotUpdate(OrderBook orderBook) {
		if (orderBook == null)
			return;
		if (orderBook.getCurrencyPair() == null)
			throw new IllegalStateException("currencyPair in orderBook can not be null");
		payloads.putIfAbsent(orderBook.getCurrencyPair().getName(), new PayloadPark(orderBook.getCurrencyPair()));
		stickyThreadPool.getSingleThreadExecutor(orderBook.getCurrencyPair().getName())
		.submit(() -> {
			try {
				PayloadPark park = payloads.get(orderBook.getCurrencyPair().getName());
				park.onSnapshot(orderBook);
			} catch (Exception e) {
				logger.error("Exception on SnapshotUpdate for {}", orderBook, e);
				error(orderBook.getCurrencyPair().getTopic(), e.getMessage());
			}
		});
	}
	
	public void suspend(final String currencyPair) {
		if (currencyPair == null)
			return;
		if (payloads.containsKey(currencyPair)) {
			stickyThreadPool.getSingleThreadExecutor(currencyPair)
			.submit(() -> {
				try {
					PayloadPark park = payloads.get(currencyPair);
					park.suspend();
				} catch (Exception e) {
					logger.error("Exception on suspend for {}", currencyPair, e);
					error(currencyPair, e.getMessage());
				}
			});
		}
	}
	
	private void emit(StreamingPayload streamingPayload) {
		if (listener != null)
			listener.onOrderedStreamingPayload(streamingPayload);
	}

	private void error(String topic, String reason) {
		logger.error("Error detected in streaming sequences: {}", reason);
		if (listener != null)
			listener.onOrderedStreamingError(topic, reason);
	}

	private class WardenTask implements Runnable {

		@Override
		public void run() {
			payloads.values().forEach(park -> {
				stickyThreadPool.getSingleThreadExecutor(park.currencyPair.getName())
					.submit(() -> {
						park.review();
				});
			});
		}
		
	}
	
	private class PayloadPark {
		
		private final AtomicLong lastSequence = new AtomicLong(-100);
		private final Object parkLock = new Object();
		private final List<StreamingPayload> parkedPayloads = new ArrayList<StreamingPayload>();
		private final CurrencyPair currencyPair;

		private long lastReviewSuccess = System.currentTimeMillis(); 

		private PayloadPark(CurrencyPair currencyPair) {
			this.currencyPair = currencyPair;
		}
		
		private void add(StreamingPayload streamingPayload) {
			synchronized(parkLock) {
				lastReviewSuccess = System.currentTimeMillis();
				if (!parkedPayloads.contains(streamingPayload))
					parkedPayloads.add(streamingPayload);
			}
		}

		private void onStreamingPayload(StreamingPayload streamingPayload) {
			if (snapshotRequired && !isSnapshotReceived()) {
				if (LoggingUtils.isDataBufferingLoggingEnabled())
					logger.info("Stashing payload {} as awaiting snapshot", streamingPayload);
				synchronized(parkLock) {
					parkedPayloads.add(streamingPayload);
					review();
				}
				return;
			}
			if (snapshotRequired && useSnapshotSequence && streamingPayload.getSequenceNumber() <= snapshotSequences.get(streamingPayload.getCurrencyPair().getName())) {
				// discard
				if (LoggingUtils.isDataBufferingLoggingEnabled())
					logger.info("Discarding payload with sequence number {} - order snapshot sequence is {}",
							streamingPayload.getSequenceNumber(), 
							snapshotSequences.get(streamingPayload.getCurrencyPair().getName()));
				return;
			}
			// case for first in on just initialised and no snapshot
			if (parkedPayloads.size() > 0) {
				add(streamingPayload);
				review();
			} else {
				lastSequence.compareAndSet(-100, streamingPayload.getSequenceNumber() - 1);
				if (lastSequence.compareAndSet(streamingPayload.getSequenceNumber() - 1, streamingPayload.getSequenceNumber())) {
					if (LoggingUtils.isDataBufferingLoggingEnabled())
						logger.info("Emitting expected payload {} with sequence number {}",
								streamingPayload,
								streamingPayload.getSequenceNumber());
					emit(streamingPayload);
					if (parkedPayloads.size() > 0)
						review();
				} else {
					if (LoggingUtils.isDataBufferingLoggingEnabled())
						logger.info("Parking unexpected {} with sequence number {}",
								streamingPayload,
								streamingPayload.getSequenceNumber());
					add(streamingPayload);
					review();
				}
			}
		}

		private void onSnapshot(OrderBook orderBook) {
			if (snapshotRequired) {
				if (LoggingUtils.isDataBufferingLoggingEnabled())
					logger.info("Received orderBook snapshot {}", orderBook);
				if (orderBook == null)
					throw new IllegalStateException("orderBook can not be null");
				if (snapshotsReceived.putIfAbsent(currencyPair.getName(), new AtomicBoolean(true)) != null)
					snapshotsReceived.get(currencyPair.getName()).set(true);
				snapshotSequences.put(currencyPair.getName(), orderBook.getSnapshotSequence());
				if (useSnapshotSequence)
					lastSequence.set(orderBook.getSnapshotSequence());
			}
			review();
		}
		
		private void suspend() {
			if (snapshotRequired) {
				if (snapshotsReceived.putIfAbsent(currencyPair.getName(), new AtomicBoolean(false)) != null)
					snapshotsReceived.get(currencyPair.getName()).set(false);
			}
		}
		
		private void review() {
			if (snapshotRequired && !isSnapshotReceived()) {
				return;
			}
			if (parkedPayloads.size() > 0) {
				synchronized(parkLock) {
					Collections.sort(parkedPayloads, SEQ_COMPARATOR);
					if (LoggingUtils.isDataBufferingLoggingEnabled())
						logger.info("Reviewing {} payloads {}", currencyPair.getName(), parkedPayloads);
					Iterator<StreamingPayload> i = parkedPayloads.iterator();
					while (i.hasNext()) {
						StreamingPayload streamingPayload = i.next();
						if (LoggingUtils.isDataBufferingLoggingEnabled())
							logger.info("Review comparing sequence number {} with last sent {}",
									streamingPayload.getSequenceNumber(), lastSequence.get());
						boolean checkSnapshotSequence = snapshotRequired && useSnapshotSequence;  
						if (checkSnapshotSequence)
							logger.info("Review comparing sequence number {} snapshotSequence {}",
									streamingPayload.getSequenceNumber(), snapshotSequences.get(currencyPair.getName()));
						if (!checkSnapshotSequence || streamingPayload.getSequenceNumber() > snapshotSequences.get(currencyPair.getName())) {
							if (lastSequence.compareAndSet(streamingPayload.getSequenceNumber() - 1, streamingPayload.getSequenceNumber())) {
								if (LoggingUtils.isDataBufferingLoggingEnabled())
									logger.info("Review found payload with sequence number {} ready for sending", streamingPayload.getSequenceNumber());
								lastReviewSuccess = System.currentTimeMillis(); 
								emit(streamingPayload);
								i.remove();
							} else if (lastSequence.get() >= streamingPayload.getSequenceNumber()) {
								error(streamingPayload.getCurrencyPair().getTopic(), String.format("Expecting sequence number %s but have parked message with sequence %s",  
										(lastSequence.get() + 1), streamingPayload.getSequenceNumber())); 
							}
						} else {
							if (LoggingUtils.isDataBufferingLoggingEnabled())
								logger.info("Discarding {} with sequence less than snapshot sequence {}",
										streamingPayload.getSequenceNumber(), snapshotSequences.get(streamingPayload.getCurrencyPair().getName()));
							i.remove();
						}
						// any left add them back to the parkedPayloads
					}
					if (parkedPayloads.size() > 0 && (System.currentTimeMillis() - lastReviewSuccess) > MAX_WAIT_ON_MISSING_SEQ) {
						error(parkedPayloads.get(0).getCurrencyPair().getTopic(), 
								String.format("Waited %s millis for sequence number %s with parked messages %s",
										(System.currentTimeMillis() - lastReviewSuccess), 
										(lastSequence.get() + 1),
										parkedPayloads)); 
					}
				}
			}
		}
		
		private boolean isSnapshotReceived() {
			if (snapshotRequired) {
				snapshotsReceived.putIfAbsent(currencyPair.getName(), new AtomicBoolean(false));
				snapshotSequences.putIfAbsent(currencyPair.getName(), -100L);
				if (!snapshotsReceived.get(currencyPair.getName()).get()) {
					return false;
				}
				return true;
			} else {
				return true;
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
