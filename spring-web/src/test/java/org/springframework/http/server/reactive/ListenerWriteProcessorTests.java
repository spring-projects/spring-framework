/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractListenerWriteProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class ListenerWriteProcessorTests {

	private final TestListenerWriteProcessor processor = new TestListenerWriteProcessor();

	private final TestResultSubscriber resultSubscriber = new TestResultSubscriber();

	private final TestSubscription subscription = new TestSubscription();


	@BeforeEach
	public void setup() {
		this.processor.subscribe(this.resultSubscriber);
		this.processor.onSubscribe(this.subscription);
		assertThat(subscription.getDemand()).isEqualTo(1);
	}


	@Test // SPR-17410
	public void writePublisherError() {

		// Turn off writing so next item will be cached
		this.processor.setWritePossible(false);
		DataBuffer buffer = mock(DataBuffer.class);
		this.processor.onNext(buffer);

		// Send error while item cached
		this.processor.onError(new IllegalStateException());

		assertThat(this.resultSubscriber.getError()).as("Error should flow to result publisher").isNotNull();
		assertThat(this.processor.getDiscardedBuffers().size()).isEqualTo(1);
		assertThat(this.processor.getDiscardedBuffers().get(0)).isSameAs(buffer);
	}

	@Test // SPR-17410
	public void ioExceptionDuringWrite() {

		// Fail on next write
		this.processor.setWritePossible(true);
		this.processor.setFailOnWrite(true);

		// Write
		DataBuffer buffer = mock(DataBuffer.class);
		this.processor.onNext(buffer);

		assertThat(this.resultSubscriber.getError()).as("Error should flow to result publisher").isNotNull();
		assertThat(this.processor.getDiscardedBuffers().size()).isEqualTo(1);
		assertThat(this.processor.getDiscardedBuffers().get(0)).isSameAs(buffer);
	}

	@Test // SPR-17410
	public void onNextWithoutDemand() {

		// Disable writing: next item will be cached..
		this.processor.setWritePossible(false);
		DataBuffer buffer1 = mock(DataBuffer.class);
		this.processor.onNext(buffer1);

		// Send more data illegally
		DataBuffer buffer2 = mock(DataBuffer.class);
		this.processor.onNext(buffer2);

		assertThat(this.resultSubscriber.getError()).as("Error should flow to result publisher").isNotNull();
		assertThat(this.processor.getDiscardedBuffers().size()).isEqualTo(2);
		assertThat(this.processor.getDiscardedBuffers().get(0)).isSameAs(buffer2);
		assertThat(this.processor.getDiscardedBuffers().get(1)).isSameAs(buffer1);
	}


	private static final class TestListenerWriteProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

		private final List<DataBuffer> discardedBuffers = new ArrayList<>();

		private boolean writePossible;

		private boolean failOnWrite;


		public List<DataBuffer> getDiscardedBuffers() {
			return this.discardedBuffers;
		}

		public void setWritePossible(boolean writePossible) {
			this.writePossible = writePossible;
		}

		public void setFailOnWrite(boolean failOnWrite) {
			this.failOnWrite = failOnWrite;
		}


		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return false;
		}

		@Override
		protected boolean isWritePossible() {
			return this.writePossible;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			if (this.failOnWrite) {
				throw new IOException("write failed");
			}
			return true;
		}

		@Override
		protected void writingFailed(Throwable ex) {
			cancel();
			onError(ex);
		}

		@Override
		protected void discardData(DataBuffer dataBuffer) {
			this.discardedBuffers.add(dataBuffer);
		}
	}


	private static final class TestSubscription implements Subscription {

		private long demand;


		public long getDemand() {
			return this.demand;
		}


		@Override
		public void request(long n) {
			this.demand = (n == Long.MAX_VALUE ? n : this.demand + n);
		}

		@Override
		public void cancel() {
		}
	}

	private static final class TestResultSubscriber implements Subscriber<Void> {

		private Throwable error;


		public Throwable getError() {
			return this.error;
		}


		@Override
		public void onSubscribe(Subscription subscription) {
		}

		@Override
		public void onNext(Void aVoid) {
		}

		@Override
		public void onError(Throwable ex) {
			this.error = ex;
		}

		@Override
		public void onComplete() {
		}
	}

}
