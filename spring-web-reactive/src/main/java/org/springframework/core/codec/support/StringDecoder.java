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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.subscriber.SubscriberBarrier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
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

	private final DataBufferAllocator allocator;

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * <p>By default, this decoder will buffer bytes and
	 * emit a single String as a result.
	 */
	public StringDecoder(DataBufferAllocator allocator) {
		this(allocator, true);
	}

	/**
	 * Create a {@code StringDecoder} that decodes a bytes stream to a String stream
	 *
	 * @param reduceToSingleBuffer whether this decoder should buffer all received items
	 * and decode a single consolidated String or re-emit items as they are provided
	 */
	public StringDecoder(DataBufferAllocator allocator, boolean reduceToSingleBuffer) {
		super(allocator, new MimeType("text", "plain", DEFAULT_CHARSET));
		this.reduceToSingleBuffer = reduceToSingleBuffer;
		this.allocator = allocator;
	}

	@Override
	public boolean canDecode(ResolvableType type, MimeType mimeType, Object... hints) {
		return super.canDecode(type, mimeType, hints)
				&& String.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public SubscriberBarrier<DataBuffer, DataBuffer> subscriberBarrier(
			Subscriber<? super DataBuffer> subscriber) {
		if (reduceToSingleBuffer) {
			return new ReduceSingleByteStreamBarrier(subscriber, allocator);
		}
		else {
			return new SubscriberBarrier<DataBuffer, DataBuffer>(subscriber);
		}

	}

	@Override
	public Flux<String> decodeInternal(Publisher<DataBuffer> inputStream,
			ResolvableType type, MimeType mimeType, Object... hints) {
		Charset charset;
		if (mimeType != null && mimeType.getCharSet() != null) {
			charset = mimeType.getCharSet();
		}
		else {
			charset = DEFAULT_CHARSET;
		}
		return Flux.from(inputStream).map(content -> {
			byte[] bytes = new byte[content.readableByteCount()];
			content.read(bytes);
			return new String(bytes, charset);
		});
	}

}
