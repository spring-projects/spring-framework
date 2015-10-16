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
import org.springframework.util.ClassUtils;

import reactor.core.subscriber.SubscriberBarrier;
import reactor.io.buffer.Buffer;
import reactor.rx.Promise;
import rx.Observable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static reactor.Publishers.*;

/**
 * Encode a bye stream of individual JSON element to a byte stream representing a single
 * JSON array when {@code Hints.ENCODE_AS_ARRAY} is enabled.
 *
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 *
 * @see JsonObjectDecoder
 */
public class JsonObjectEncoder implements MessageToByteEncoder<ByteBuffer> {

	private static final boolean rxJava1Present =
			ClassUtils.isPresent("rx.Observable", JsonObjectEncoder.class.getClassLoader());

	private static final boolean reactorPresent =
			ClassUtils.isPresent("reactor.rx.Promise", JsonObjectEncoder.class.getClassLoader());

	final ByteBuffer START_ARRAY = ByteBuffer.wrap("[".getBytes());

	final ByteBuffer END_ARRAY = ByteBuffer.wrap("]".getBytes());

	final ByteBuffer COMMA = ByteBuffer.wrap(",".getBytes());


	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON) &&
				!(reactorPresent && Promise.class.isAssignableFrom(type.getRawClass())) &&
				(rxJava1Present && Observable.class.isAssignableFrom(type.getRawClass())
				|| Publisher.class.isAssignableFrom(type.getRawClass()));
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends ByteBuffer> messageStream, ResolvableType type, MediaType
	  mediaType, Object... hints) {
		//TODO Merge some chunks, there is no need to have chunks with only '[', ']' or ',' characters
		return
		  concat(
			  from(
			    Arrays.<Publisher<ByteBuffer>>asList(
				  just(START_ARRAY),
				  lift(
				    flatMap(messageStream, (ByteBuffer b) -> from(Arrays.asList(b, COMMA))),
				    sub -> new SkipLastBarrier(sub)
				  ),
				  just(END_ARRAY)
			    )
			  )
		  );
	}

	private static class SkipLastBarrier extends SubscriberBarrier<ByteBuffer, ByteBuffer> {

		public SkipLastBarrier(Subscriber<? super ByteBuffer> subscriber) {
			super(subscriber);
		}

		ByteBuffer prev = null;

		@Override
		protected void doNext(ByteBuffer next) {
			if (prev == null) {
				prev = next;
				doRequest(1);
				return;
			}

			ByteBuffer tmp = prev;
			prev = next;
			subscriber.onNext(tmp);
		}

	}
}
