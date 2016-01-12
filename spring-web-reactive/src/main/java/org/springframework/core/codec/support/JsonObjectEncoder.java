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

package org.springframework.core.codec.support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.Flux;
import reactor.Mono;
import reactor.core.subscriber.SubscriberBarrier;
import reactor.core.support.BackpressureUtils;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
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
public class JsonObjectEncoder extends AbstractEncoder<ByteBuffer> {

	public JsonObjectEncoder() {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
	}

	@Override
	public Flux<ByteBuffer> encode(Publisher<? extends ByteBuffer> inputStream,
			ResolvableType type, MimeType mimeType, Object... hints) {

		if (inputStream instanceof Mono) {
			return Flux.from(inputStream);
		}
		return Flux.from(inputStream).lift(s -> new JsonArrayEncoderBarrier(s));
	}


	private static class JsonArrayEncoderBarrier extends SubscriberBarrier<ByteBuffer, ByteBuffer> {

		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<JsonArrayEncoderBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(JsonArrayEncoderBarrier.class, "requested");

		static final AtomicIntegerFieldUpdater<JsonArrayEncoderBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(JsonArrayEncoderBarrier.class, "terminated");


		private ByteBuffer prev = null;

		private long count = 0;

		private volatile long requested;

		private volatile int terminated;


		public JsonArrayEncoderBarrier(Subscriber<? super ByteBuffer> subscriber) {
			super(subscriber);
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
		protected void doNext(ByteBuffer next) {
			this.count++;

			ByteBuffer tmp = this.prev;
			this.prev = next;
			Buffer buffer = new Buffer();
			if (this.count == 1) {
				buffer.append("[");
			}
			if (tmp != null) {
				buffer.append(tmp);
			}
			if (this.count > 1) {
				buffer.append(",");
			}
			buffer.flip();

			BackpressureUtils.getAndSub(REQUESTED, this, 1L);
			subscriber.onNext(buffer.byteBuffer());
		}

		protected void drainLast(){
			if(BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				Buffer buffer = new Buffer();
				buffer.append(this.prev);
				buffer.append("]");
				buffer.flip();
				subscriber.onNext(buffer.byteBuffer());
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
