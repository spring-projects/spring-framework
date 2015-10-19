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

import static reactor.Publishers.*;
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
		return lift(messageStream, sub -> new JsonEncoderBarrier(sub));
	}

	private static class JsonEncoderBarrier extends SubscriberBarrier<ByteBuffer, ByteBuffer> {

		public JsonEncoderBarrier(Subscriber<? super ByteBuffer> subscriber) {
			super(subscriber);
		}

		ByteBuffer prev = null;
		long count = 0;

		@Override
		protected void doNext(ByteBuffer next) {
			count++;
			if (count == 1) {
				prev = next;
				doRequest(1);
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
			subscriber.onNext(buffer.byteBuffer());
		}

		@Override
		protected void doComplete() {
			Buffer buffer = new Buffer();
			buffer.append(prev);
			if (count > 1) {
				buffer.append("]");
			}
			buffer.flip();
			subscriber.onNext(buffer.byteBuffer());
			subscriber.onComplete();
		}
	}

}
