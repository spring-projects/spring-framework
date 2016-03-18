/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.core.codec.support;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.core.util.BackpressureUtils;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.MimeType;

/**
 * Encode a byte stream of individual JSON element to a byte stream representing:
 *  - the same JSON object than the input stream if it is a {@link Mono}
 *  - a JSON array for other kinds of {@link Publisher}
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 *
 * @see JsonObjectDecoder
 */
public class JsonObjectEncoder extends AbstractEncoder<DataBuffer> {

	public JsonObjectEncoder() {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends DataBuffer> inputStream,
			DataBufferAllocator allocator,
			ResolvableType type, MimeType mimeType, Object... hints) {
		if (inputStream instanceof Mono) {
			return Flux.from(inputStream);
		}
		return Flux.from(inputStream)
				.lift(s -> new JsonArrayEncoderBarrier(s, allocator));
	}

	private static class JsonArrayEncoderBarrier
			extends SubscriberBarrier<DataBuffer, DataBuffer> {

		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<JsonArrayEncoderBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(JsonArrayEncoderBarrier.class, "requested");

		static final AtomicIntegerFieldUpdater<JsonArrayEncoderBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(JsonArrayEncoderBarrier.class, "terminated");

		private final DataBufferAllocator allocator;

		private DataBuffer prev = null;

		private long count = 0;

		private volatile long requested;

		private volatile int terminated;

		public JsonArrayEncoderBarrier(Subscriber<? super DataBuffer> subscriber,
				DataBufferAllocator allocator) {
			super(subscriber);
			this.allocator = allocator;
		}


		@Override
		protected void doRequest(long n) {
			BackpressureUtils.getAndAdd(REQUESTED, this, n);
			if(TERMINATED.compareAndSet(this, 1, 2)){
				drainLast();
			}
			else {
				super.doRequest(n);
			}
		}

		@Override
		protected void doNext(DataBuffer next) {
			this.count++;

			DataBuffer tmp = this.prev;
			this.prev = next;
			DataBuffer buffer = allocator.allocateBuffer();
			if (this.count == 1) {
				buffer.write((byte) '[');
			}
			if (tmp != null) {
				buffer.write(tmp);
			}
			if (this.count > 1) {
				buffer.write((byte) ',');
			}

			BackpressureUtils.getAndSub(REQUESTED, this, 1L);
			subscriber.onNext(buffer);
		}

		protected void drainLast(){
			if(BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				DataBuffer buffer = allocator.allocateBuffer();
				buffer.write(this.prev);
				buffer.write((byte) ']');
				subscriber.onNext(buffer);
				super.doComplete();
			}
		}

		@Override
		protected void doComplete() {
			if(TERMINATED.compareAndSet(this, 0, 1)) {
				drainLast();
			}
		}
	}

}
