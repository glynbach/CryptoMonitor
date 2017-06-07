package com.kieral.cryptomon.streaming;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.JsonNode;
import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.model.OrderBook;
import com.kieral.cryptomon.service.util.LoggingUtils;

public class TestOrderedStreamingEmitter {

	CountDownLatch testLatch;
	OrderedStreamingEmitter emitter;
	IOrderedStreamingListener listener;
	List<StreamingPayload> emitted;
	List<String> errors;
	int poolSize = 2;
	
	@Before
	public void setUp() {
		LoggingUtils.setDataBufferingLoggingEnabled(true);
		listener = Mockito.mock(IOrderedStreamingListener.class);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				testLatch.countDown();
				emitted.add(invocation.getArgument(0));
				return null;
			}}).when(listener).onOrderedStreamingPayload(Mockito.any(StreamingPayload.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				testLatch.countDown();
				errors.add(invocation.getArgument(1));
				return null;
			}}).when(listener).onOrderedStreamingError(Mockito.anyString(), Mockito.anyString());
		emitted = Collections.synchronizedList(new ArrayList<StreamingPayload>());
		errors = Collections.synchronizedList(new ArrayList<String>());
	}

	private void init(int latchEventCount) {
		init(latchEventCount, false);
	}

	private void init(int latchEventCount, boolean requiresSnasphot) {
		emitter = new OrderedStreamingEmitter("Test", listener, requiresSnasphot, true, poolSize);
		testLatch = new CountDownLatch(latchEventCount);
	}
	
	@Test
	public void testInOrderMessages() throws InterruptedException {
		init(5);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testOutOfOrderMessages() throws InterruptedException {
		init(5);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testDrippingOutOfOrderMessages() throws InterruptedException {
		init(5);
		emitter.onStreamingUpdate(pl(1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(5));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testManyDrippingOutOfOrderMessages() throws InterruptedException {
		init(15);
		emitter.onStreamingUpdate(pl(1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(6));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(7));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(9));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(8));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(13));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(10));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(12));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(11));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(15));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(14));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(15, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testMissedMessages() throws InterruptedException {
		init(5);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		testLatch.await(5000, TimeUnit.MILLISECONDS);
		assertEquals(1, emitted.size());
		assertEquals(1, emitted.get(0).getSequenceNumber());
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Waited"));
	}

	@Test
	public void testMessagesOfEqualSequence() throws InterruptedException {
		init(4);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Expecting"));
	}

	@Test
	public void testMessagesOfLowerSequence() throws InterruptedException {
		init(4);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(2));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Expecting"));
	}

	@Test
	public void testInOrderMultiCcyMessages() throws InterruptedException {
		init(6);
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("B", 3));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(6, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().getName().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().getName().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testOutOfOrderMultipleCcyMessages() throws InterruptedException {
		init(8);
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("B", 4));
		emitter.onStreamingUpdate(pl("A", 4));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(8, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().getName().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().getName().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testDrippingOutOfOrderMultipleCcyMessages() throws InterruptedException {
		init(8);
		emitter.onStreamingUpdate(pl("A", 1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 4));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(8, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().getName().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().getName().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testManyDrippingOutOfOrderMultipleCcyMessages() throws InterruptedException {
		init(16);
		emitter.onStreamingUpdate(pl("A", 1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 5));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 7));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 6));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 7));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 6));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 8));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("B", 8));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl("A", 5));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(16, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().getName().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().getName().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testMissedMessagesMultipleCcy() throws InterruptedException {
		init(4);
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("A", 3));
		testLatch.await(5000, TimeUnit.MILLISECONDS);
		assertEquals(2, emitted.size());
		assertEquals(1, emitted.get(0).getSequenceNumber());
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Waited"));
	}
	
	@Test
	public void testMessagesOfLowerSequenceMultipleCcy() throws InterruptedException {
		init(8);
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Expecting"));
	}

	private StreamingPayload pl(final long seq) {
		return new StreamingPayload() {
			@Override
			public long getSequenceNumber() {
				return seq;
			}
			@Override
			public CurrencyPair getCurrencyPair() {
				return new CurrencyPair("CP","CP");
			}
			@Override
			public JsonNode getJson() {
				return null;
			}
			@Override
			public String toString() {
				return "CP:CP SEQ:" + seq;
			}
			@Override
			public String getRaw() {
				return "CP:CP SEQ:" + seq;
			}
			@Override
			public boolean equals(Object obj) {
				if (obj instanceof StreamingPayload) {
					return getRaw().equals(((StreamingPayload)obj).getRaw());
				}
				return super.equals(obj);
			}
			@Override
			public int hashCode() {
				return getRaw().hashCode();
			}
			@Override
			public boolean isHeartbeat() {
				return false;
			}
		};
	}

	private StreamingPayload pl(final String cp, final long seq) {
		return new StreamingPayload() {
			@Override
			public long getSequenceNumber() {
				return seq;
			}
			@Override
			public CurrencyPair getCurrencyPair() {
				return new CurrencyPair(cp, cp);
			}
			@Override
			public JsonNode getJson() {
				return null;
			}
			@Override
			public String toString() {
				return "CP:" + cp + " SEQ:" + seq;
			}
			@Override
			public String getRaw() {
				return "CP:" + cp + " SEQ:" + seq;
			}
			@Override
			public boolean equals(Object obj) {
				if (obj instanceof StreamingPayload) {
					return getRaw().equals(((StreamingPayload)obj).getRaw());
				}
				return super.equals(obj);
			}
			@Override
			public int hashCode() {
				return getRaw().hashCode();
			}
			@Override
			public boolean isHeartbeat() {
				return false;
			}
		};
	}

	@Test
	public void testRequiresSnapshot() throws InterruptedException {
		init(3, true);
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(500);
		assertEquals(0, errors.size());
		assertEquals(0, emitted.size());
		emitter.onSnashotUpdate(ob(4));
		emitter.onStreamingUpdate(pl(6));
		emitter.onStreamingUpdate(pl(7));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(3, emitted.size());
		AtomicInteger seq = new AtomicInteger(4);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testRequiresSnapshotMultipleCcy() throws InterruptedException {
		init(5, true);
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("B", 1));
		Thread.sleep(500);
		assertEquals(0, errors.size());
		assertEquals(0, emitted.size());
		emitter.onSnashotUpdate(ob("A", 0));
		emitter.onStreamingUpdate(pl("A", 4));
		emitter.onSnashotUpdate(ob("B", 2));
		emitter.onStreamingUpdate(pl("A", 3));
		testLatch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger aSeq = new AtomicInteger(0);
		AtomicInteger BSeq = new AtomicInteger(2);
		emitted.forEach(payload -> {
			if (payload.getCurrencyPair().getName().equals("A"))
				assertEquals(aSeq.incrementAndGet(), payload.getSequenceNumber());
			else
				assertEquals(BSeq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	private OrderBook ob(long seq) {
		return ob("CP", seq);
	}

	private OrderBook ob(String cp, long seq) {
		OrderBook ob = new OrderBook("TEST", new CurrencyPair(cp, cp));
		ob.setSnapshotSequence(seq);
		return ob;
	}

}
