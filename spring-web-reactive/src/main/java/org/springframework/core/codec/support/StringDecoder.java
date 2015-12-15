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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.Flux;
import reactor.core.subscriber.SubscriberBarrier;

import org.springframework.core.ResolvableType;
import org.springframework.util.MimeType;

/**
 * Decode from a bytes stream to a String stream.
 *
 * <p>By default, this decoder will buffer the received elements into a single
 * {@code ByteBuffer} and will emit a single {@code String} once the stream of
 * elements is complete. This behavior can be turned off using an constructor
 * argument but the {@code Subcriber} should pay attention to split characters
 * issues.
 *
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @see StringEncoder
 */
public class StringDecoder extends AbstractRawByteStreamDecoder<String> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public final boolean reduceToSingleBuffer;

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * <p>By default, this decoder will buffer bytes and
	 * emit a single String as a result.
	 */
	public StringDecoder() {
		this(true);
	}

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * @param reduceToSingleBuffer whether this decoder should buffer all received items
	 * and decode a single consolidated String or re-emit items as they are provided
	 */
	public StringDecoder(boolean reduceToSingleBuffer) {
		super(new MimeType("text", "plain", DEFAULT_CHARSET));
		this.reduceToSingleBuffer = reduceToSingleBuffer;
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		return super.canDecode(type, mimeType, hints)
				&& String.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public SubscriberBarrier<ByteBuffer, ByteBuffer> subscriberBarrier(Subscriber<? super ByteBuffer> subscriber) {
		if (reduceToSingleBuffer) {
			return new ReduceSingleByteStreamBarrier(subscriber);
		}
		else {
			return new SubscriberBarrier(subscriber);
		}

	}

	@Override
	public Flux<String> decodeInternal(Publisher<ByteBuffer> inputStream, ResolvableType type, MimeType mimeType, Object... hints) {
		Charset charset;
		if (mimeType != null && mimeType.getCharSet() != null) {
			charset = mimeType.getCharSet();
		}
		else {
			charset = DEFAULT_CHARSET;
		}
		return Flux.from(inputStream).map(content -> new String(content.duplicate().array(), charset));
	}

}
