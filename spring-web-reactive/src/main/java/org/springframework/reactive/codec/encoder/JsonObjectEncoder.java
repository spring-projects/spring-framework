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

package org.springframework.reactive.codec.encoder;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.decoder.JsonObjectDecoder;

import reactor.core.subscriber.SubscriberBarrier;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static reactor.Publishers.*;

import reactor.core.support.BackpressureUtils;
import reactor.io.buffer.Buffer;

/**
 * Encode a byte stream of individual JSON element to a byte stream representing a single
 * JSON array when if it contains more than one element.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 *
 * @see JsonObjectDecoder
 */
public class JsonObjectEncoder implements MessageToByteEncoder<ByteBuffer> {


	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends ByteBuffer> messageStream,
			ResolvableType type, MediaType mediaType, Object... hints) {
		return lift(messageStream, bbs -> new JsonEncoderBarrier(bbs));
	}

	private static class JsonEncoderBarrier extends SubscriberBarrier<ByteBuffer, ByteBuffer> {

		private volatile long requested;
		@SuppressWarnings("rawtypes")
		static final AtomicLongFieldUpdater<JsonEncoderBarrier> REQUESTED =
				AtomicLongFieldUpdater.newUpdater(JsonEncoderBarrier.class, "requested");

		private volatile int terminated;
		static final AtomicIntegerFieldUpdater<JsonEncoderBarrier> TERMINATED =
				AtomicIntegerFieldUpdater.newUpdater(JsonEncoderBarrier.class, "terminated");

		ByteBuffer prev = null;
		long count = 0;

		public JsonEncoderBarrier(Subscriber<? super ByteBuffer> subscriber) {
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
			count++;
			if (count == 1) {
				prev = next;
				super.doRequest(1);
				return;
			}

			ByteBuffer tmp = prev;
			prev = next;
			Buffer buffer = new Buffer();
			if (count == 2) {
				buffer.append("[");
			}
			buffer.append(tmp);
			buffer.append(",");
			buffer.flip();

			BackpressureUtils.getAndSub(REQUESTED, this, 1L);
			subscriber.onNext(buffer.byteBuffer());
		}

		protected void drainLast(){
			if(BackpressureUtils.getAndSub(REQUESTED, this, 1L) > 0) {
				Buffer buffer = new Buffer();
				buffer.append(prev);
				if (count > 1) {
					buffer.append("]");
				}
				buffer.flip();
				subscriber.onNext(buffer.byteBuffer());
				super.doComplete();
			}
		}

		@Override
		protected void doComplete() {
			if(TERMINATED.compareAndSet(this, 0, 1)){
				drainLast();
			}
		}
	}

}
