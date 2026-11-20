/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractListenerReadPublisher}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 */
class ListenerReadPublisherTests {

	private final TestListenerReadPublisher publisher = new TestListenerReadPublisher();

	private final TestSubscriber subscriber = new TestSubscriber();


	@BeforeEach
	void setup() {
		this.publisher.subscribe(this.subscriber);
	}


	@Test
	void twoReads() {

		this.subscriber.getSubscription().request(2);
		this.publisher.onDataAvailable();

		assertThat(this.publisher.getReadCalls()).isEqualTo(2);
	}

	@Test // SPR-17410
	void discardDataOnError() {

		this.subscriber.getSubscription().request(2);
		this.publisher.onDataAvailable();
		this.publisher.onError(new IllegalStateException());

		assertThat(this.publisher.getReadCalls()).isEqualTo(2);
		assertThat(this.publisher.getDiscardCalls()).isEqualTo(1);
	}

	@Test // SPR-17410
	void discardDataOnCancel() {

		this.subscriber.getSubscription().request(2);
		this.subscriber.setCancelOnNext(true);
		this.publisher.onDataAvailable();

		assertThat(this.publisher.getReadCalls()).isEqualTo(1);
		assertThat(this.publisher.getDiscardCalls()).isEqualTo(1);
	}


	private static final class TestListenerReadPublisher extends AbstractListenerReadPublisher<DataBuffer> {

		private int readCalls = 0;

		private int discardCalls = 0;


		public TestListenerReadPublisher() {
			super("");
		}


		public int getReadCalls() {
			return this.readCalls;
		}

		public int getDiscardCalls() {
			return this.discardCalls;
		}

		@Override
		protected void checkOnDataAvailable() {
			// no-op
		}

		@Override
		protected @Nullable DataBuffer read() {
			if (this.discardCalls != 0) {
				return null;
			}
			this.readCalls++;
			return mock();
		}

		@Override
		protected void readingPaused() {
			// No-op
		}

		@Override
		protected void discardData() {
			this.discardCalls++;
		}
	}


	private static final class TestSubscriber implements Subscriber<DataBuffer> {

		private Subscription subscription;

		private boolean cancelOnNext;


		public Subscription getSubscription() {
			return this.subscription;
		}

		public void setCancelOnNext(boolean cancelOnNext) {
			this.cancelOnNext = cancelOnNext;
		}


		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
		}

		@Override
		public void onNext(DataBuffer dataBuffer) {
			if (this.cancelOnNext) {
				this.subscription.cancel();
			}
		}

		@Override
		public void onError(Throwable t) {
		}

		@Override
		public void onComplete() {
		}
	}

}
