/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.http.server.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.Flux;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.rx.Streams;
import reactor.rx.stream.Signal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class WriteWithOperatorTests {

	private OneByOneAsyncWriter writer;

	private WriteWithOperator<String> operator;


	@Before
	public void setUp() throws Exception {
		this.writer = new OneByOneAsyncWriter();
		this.operator = new WriteWithOperator<>(this.writer::writeWith);
	}

	@Test
	public void errorBeforeFirstItem() throws Exception {
		IllegalStateException error = new IllegalStateException("boo");
		Publisher<Void> completion = Flux.<String>error(error).lift(this.operator);
		List<Signal<Void>> signals = Streams.from(completion).materialize().toList().get();

		assertEquals(1, signals.size());
		assertSame("Unexpected signal: " + signals.get(0), error, signals.get(0).getThrowable());
	}

	@Test
	public void completionBeforeFirstItem() throws Exception {
		Publisher<Void> completion = Flux.<String>empty().lift(this.operator);
		List<Signal<Void>> signals = Streams.from(completion).materialize().toList().get();

		assertEquals(1, signals.size());
		assertTrue("Unexpected signal: " + signals.get(0), signals.get(0).isOnComplete());

		assertEquals(0, this.writer.items.size());
		assertTrue(this.writer.completed);
	}

	@Test
	public void writeOneItem() throws Exception {
		Publisher<Void> completion = Flux.just("one").lift(this.operator);
		List<Signal<Void>> signals = Streams.from(completion).materialize().toList().get();

		assertEquals(1, signals.size());
		assertTrue("Unexpected signal: " + signals.get(0), signals.get(0).isOnComplete());

		assertEquals(1, this.writer.items.size());
		assertEquals("one", this.writer.items.get(0));
		assertTrue(this.writer.completed);
	}


	@Test
	public void writeMultipleItems() throws Exception {
		List<String> items = Arrays.asList("one", "two", "three");
		Publisher<Void> completion = Flux.fromIterable(items).lift(this.operator);
		List<Signal<Void>> signals = Streams.from(completion).materialize().toList().get();

		assertEquals(1, signals.size());
		assertTrue("Unexpected signal: " + signals.get(0), signals.get(0).isOnComplete());

		assertEquals(3, this.writer.items.size());
		assertEquals("one", this.writer.items.get(0));
		assertEquals("two", this.writer.items.get(1));
		assertEquals("three", this.writer.items.get(2));
		assertTrue(this.writer.completed);
	}

	@Test
	public void errorAfterMultipleItems() throws Exception {
		IllegalStateException error = new IllegalStateException("boo");
		Flux<String> publisher = Flux.create(subscriber -> {
			int i = subscriber.context().incrementAndGet();
			subscriber.onNext(String.valueOf(i));
			if (i == 3) {
				subscriber.onError(error);
			}
		}, subscriber -> new AtomicInteger());
		Publisher<Void> completion = publisher.lift(this.operator);
		List<Signal<Void>> signals = Streams.from(completion).materialize().toList().get();

		assertEquals(1, signals.size());
		assertSame("Unexpected signal: " + signals.get(0), error, signals.get(0).getThrowable());

		assertEquals(3, this.writer.items.size());
		assertEquals("1", this.writer.items.get(0));
		assertEquals("2", this.writer.items.get(1));
		assertEquals("3", this.writer.items.get(2));
		assertSame(error, this.writer.error);
	}


	private static class OneByOneAsyncWriter {

		private List<String> items = new ArrayList<>();

		private boolean completed = false;

		private Throwable error;


		public Publisher<Void> writeWith(Publisher<String> publisher) {
			return subscriber -> {
				Executors.newSingleThreadScheduledExecutor().schedule(
						(Runnable) () -> publisher.subscribe(new WriteSubscriber(subscriber)),
						50, TimeUnit.MILLISECONDS);
			};
		}

		private class WriteSubscriber extends SubscriberBarrier<String, Void> {

			public WriteSubscriber(Subscriber<? super Void> subscriber) {
				super(subscriber);
			}

			@Override
			protected void doOnSubscribe(Subscription subscription) {
				subscription.request(1);
			}

			@Override
			public void doNext(String item) {
				items.add(item);
				this.subscription.request(1);
			}

			@Override
			public void doError(Throwable ex) {
				error = ex;
				this.subscriber.onError(ex);
			}

			@Override
			public void doComplete() {
				completed = true;
				this.subscriber.onComplete();
			}
		}
	}

	private final static Subscription NO_OP_SUBSCRIPTION = new Subscription() {

		@Override
		public void request(long n) {
		}

		@Override
		public void cancel() {
		}
	};

}
