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

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

import rx.Observable;
import rx.RxReactiveStreams;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.decoder.JsonObjectDecoder;

/**
 * Encode a bye stream of individual JSON element to a byte stream representing a single
 * JSON array when {@code Hints.ENCODE_AS_ARRAY} is enabled.
 *
 * @author Sebastien Deleuze
 * @see JsonObjectDecoder
 */
public class JsonObjectEncoder implements MessageToByteEncoder<ByteBuffer> {

	private final ByteBuffer START_ARRAY = ByteBuffer.wrap("[".getBytes());
	private final ByteBuffer END_ARRAY = ByteBuffer.wrap("]".getBytes());
	private final ByteBuffer COMMA = ByteBuffer.wrap(",".getBytes());


	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON) &&
				(Observable.class.isAssignableFrom(type.getRawClass()) || Publisher.class.isAssignableFrom(type.getRawClass()));
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends ByteBuffer> messageStream, ResolvableType type, MediaType mediaType, Object... hints) {
		// TODO We use RxJava Observable because there is no skipLast() operator in Reactor
		// TODO Merge some chunks, there is no need to have chunks with only '[', ']' or ',' characters
		return RxReactiveStreams.toPublisher(
				Observable.concat(
						Observable.just(START_ARRAY),
						RxReactiveStreams.toObservable(messageStream).flatMap(b -> Observable.just(b, COMMA)).skipLast(1),
						Observable.just(END_ARRAY)));
	}

}
