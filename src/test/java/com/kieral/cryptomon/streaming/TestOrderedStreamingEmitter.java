package com.kieral.cryptomon.streaming;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.JsonNode;

public class TestOrderedStreamingEmitter {

	OrderedStreamingEmitter emitter;
	IOrderedStreamingListener listener;
	List<StreamingPayload> emitted;
	List<String> errors;
	
	@Before
	public void setUp() {
		listener = Mockito.mock(IOrderedStreamingListener.class);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				emitted.add(invocation.getArgument(0));
				return null;
			}}).when(listener).onOrderedStreamingPayload(Mockito.any(StreamingPayload.class));
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				errors.add(invocation.getArgument(0));
				return null;
			}}).when(listener).onError(Mockito.anyString());
		emitted = new ArrayList<StreamingPayload>();
		errors = new ArrayList<String>();
		emitter = new OrderedStreamingEmitter(listener);
	}
	
	@Test
	public void testInOrderMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testOutOfOrderMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testDrippingOutOfOrderMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(3));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(2));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(4));
		Thread.sleep(1);
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(5, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testManyDrippingOutOfOrderMessages() throws InterruptedException {
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
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(15, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testMissedMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(4));
		emitter.onStreamingUpdate(pl(5));
		Thread.sleep(1500);
		assertEquals(1, emitted.size());
		assertEquals(1, emitted.get(0).getSequenceNumber());
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Waited"));
	}

	@Test
	public void testMessagesOfEqualSequence() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		Thread.sleep(100);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Expecting"));
	}

	@Test
	public void testMessagesOfLowerSequence() throws InterruptedException {
		emitter.onStreamingUpdate(pl(1));
		emitter.onStreamingUpdate(pl(2));
		emitter.onStreamingUpdate(pl(3));
		emitter.onStreamingUpdate(pl(2));
		Thread.sleep(100);
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Expecting"));
	}

	@Test
	public void testInOrderMultiCcyMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("B", 3));
		Thread.sleep(100);
		assertEquals(6, emitted.size());
		assertEquals(1, emitted.get(0).getSequenceNumber());
		assertEquals(2, emitted.get(1).getSequenceNumber());
		assertEquals(1, emitted.get(2).getSequenceNumber());
		assertEquals(3, emitted.get(3).getSequenceNumber());
		assertEquals(2, emitted.get(4).getSequenceNumber());
		assertEquals(3, emitted.get(5).getSequenceNumber());
		assertEquals("A", emitted.get(0).getCurrencyPair());
		assertEquals("A", emitted.get(1).getCurrencyPair());
		assertEquals("B", emitted.get(2).getCurrencyPair());
		assertEquals("A", emitted.get(3).getCurrencyPair());
		assertEquals("B", emitted.get(4).getCurrencyPair());
		assertEquals("B", emitted.get(5).getCurrencyPair());
	}

	@Test
	public void testOutOfOrderMultipleCcyMessages() throws InterruptedException {
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("B", 4));
		emitter.onStreamingUpdate(pl("A", 4));
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(8, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testDrippingOutOfOrderMultipleCcyMessages() throws InterruptedException {
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
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(8, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testManyDrippingOutOfOrderMultipleCcyMessages() throws InterruptedException {
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
		Thread.sleep(100);
		assertEquals(0, errors.size());
		assertEquals(16, emitted.size());
		AtomicInteger seq = new AtomicInteger(0);
		emitted.stream()
			.filter(payload -> payload.getCurrencyPair().equals("A"))
			.collect(Collectors.toList()).forEach(payload -> {
				assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
			});
		seq.set(0);
		emitted.stream()
		.filter(payload -> payload.getCurrencyPair().equals("B"))
		.collect(Collectors.toList()).forEach(payload -> {
			assertEquals(seq.incrementAndGet(), payload.getSequenceNumber());
		});
	}

	@Test
	public void testMissedMessagesMultipleCcy() throws InterruptedException {
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("A", 3));
		Thread.sleep(1500);
		assertEquals(2, emitted.size());
		assertEquals(1, emitted.get(0).getSequenceNumber());
		assertTrue(errors.size() > 0);
		assertTrue(errors.get(0).contains("Waited"));
	}
	@Test
	public void testMessagesOfLowerSequenceMultipleCcy() throws InterruptedException {
		emitter.onStreamingUpdate(pl("A", 1));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		emitter.onStreamingUpdate(pl("A", 3));
		emitter.onStreamingUpdate(pl("B", 2));
		emitter.onStreamingUpdate(pl("B", 3));
		emitter.onStreamingUpdate(pl("B", 1));
		emitter.onStreamingUpdate(pl("A", 2));
		Thread.sleep(100);
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
			public String getCurrencyPair() {
				return "CP";
			}
			@Override
			public JsonNode getJson() {
				return null;
			}
			@Override
			public String toString() {
				return "CP:CP SEQ:" + seq;
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
			public String getCurrencyPair() {
				return cp;
			}
			@Override
			public JsonNode getJson() {
				return null;
			}
			@Override
			public String toString() {
				return "CP:" + cp + " SEQ:" + seq;
			}
		};
	}
	
}
