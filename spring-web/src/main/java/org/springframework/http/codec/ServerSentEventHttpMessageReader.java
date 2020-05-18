/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;

/**
 * Reader that supports a stream of {@link ServerSentEvent ServerSentEvents} and also plain
 * {@link Object Objects} which is the same as an {@link ServerSentEvent} with data only.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {

	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	@Nullable
	private final Decoder<?> decoder;

	private final StringDecoder lineDecoder = StringDecoder.textPlainOnly();


	/**
	 * Constructor without a {@code Decoder}. In this mode only {@code String}
	 * is supported as the data of an event.
	 */
	public ServerSentEventHttpMessageReader() {
		this(null);
	}

	/**
	 * Constructor with JSON {@code Decoder} for decoding to Objects.
	 * Support for decoding to {@code String} event data is built-in.
	 */
	public ServerSentEventHttpMessageReader(@Nullable Decoder<?> decoder) {
		this.decoder = decoder;
	}


	/**
	 * Return the configured {@code Decoder}.
	 */
	@Nullable
	public Decoder<?> getDecoder() {
		return this.decoder;
	}

	/**
	 * Configure a limit on the maximum number of bytes per SSE event which are
	 * buffered before the event is parsed.
	 * <p>Note that the {@link #getDecoder() data decoder}, if provided, must
	 * also be customized accordingly to raise the limit if necessary in order
	 * to be able to parse the data portion of the event.
	 * <p>By default this is set to 256K.
	 * @param byteCount the max number of bytes to buffer, or -1 for unlimited
	 * @since 5.1.13
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.lineDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * Return the {@link #setMaxInMemorySize configured} byte count limit.
	 * @since 5.1.13
	 */
	public int getMaxInMemorySize() {
		return this.lineDecoder.getMaxInMemorySize();
	}


	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return (MediaType.TEXT_EVENT_STREAM.includes(mediaType) || isServerSentEvent(elementType));
	}

	private boolean isServerSentEvent(ResolvableType elementType) {
		return ServerSentEvent.class.isAssignableFrom(elementType.toClass());
	}


	@Override
	public Flux<Object> read(
			ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

		LimitTracker limitTracker = new LimitTracker();

		boolean shouldWrap = isServerSentEvent(elementType);
		ResolvableType valueType = (shouldWrap ? elementType.getGeneric() : elementType);

		return this.lineDecoder.decode(message.getBody(), STRING_TYPE, null, hints)
				.doOnNext(limitTracker::afterLineParsed)
				.bufferUntil(String::isEmpty)
				.concatMap(lines -> {
					Object event = buildEvent(lines, valueType, shouldWrap, hints);
					return (event != null ? Mono.just(event) : Mono.empty());
				});
	}

	@Nullable
	private Object buildEvent(List<String> lines, ResolvableType valueType, boolean shouldWrap,
			Map<String, Object> hints) {

		ServerSentEvent.Builder<Object> sseBuilder = shouldWrap ? ServerSentEvent.builder() : null;
		StringBuilder data = null;
		StringBuilder comment = null;

		for (String line : lines) {
			if (line.startsWith("data:")) {
				data = (data != null ? data : new StringBuilder());
				data.append(line.substring(5).trim()).append("\n");
			}
			if (shouldWrap) {
				if (line.startsWith("id:")) {
					sseBuilder.id(line.substring(3).trim());
				}
				else if (line.startsWith("event:")) {
					sseBuilder.event(line.substring(6).trim());
				}
				else if (line.startsWith("retry:")) {
					sseBuilder.retry(Duration.ofMillis(Long.parseLong(line.substring(6).trim())));
				}
				else if (line.startsWith(":")) {
					comment = (comment != null ? comment : new StringBuilder());
					comment.append(line.substring(1).trim()).append("\n");
				}
			}
		}

		Object decodedData = (data != null ? decodeData(data, valueType, hints) : null);

		if (shouldWrap) {
			if (comment != null) {
				sseBuilder.comment(comment.substring(0, comment.length() - 1));
			}
			if (decodedData != null) {
				sseBuilder.data(decodedData);
			}
			return sseBuilder.build();
		}
		else {
			return decodedData;
		}
	}

	@Nullable
	private Object decodeData(StringBuilder data, ResolvableType dataType, Map<String, Object> hints) {
		if (String.class == dataType.resolve()) {
			return data.substring(0, data.length() - 1);
		}
		if (this.decoder == null) {
			throw new CodecException("No SSE decoder configured and the data is not String.");
		}
		byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = bufferFactory.wrap(bytes);  // wrapping only, no allocation
		return this.decoder.decode(buffer, dataType, MediaType.TEXT_EVENT_STREAM, hints);
	}

	@Override
	public Mono<Object> readMono(
			ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {

		// In order of readers, we're ahead of String + "*/*"
		// If this is called, simply delegate to StringDecoder

		if (elementType.resolve() == String.class) {
			Flux<DataBuffer> body = message.getBody();
			return this.lineDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
		}

		return Mono.error(new UnsupportedOperationException(
				"ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
	}


	private class LimitTracker {

		private int accumulated = 0;

		public void afterLineParsed(String line) {
			if (getMaxInMemorySize() < 0) {
				return;
			}
			if (line.isEmpty()) {
				this.accumulated = 0;
			}
			if (line.length() > Integer.MAX_VALUE - this.accumulated) {
				raiseLimitException();
			}
			else {
				this.accumulated += line.length();
				if (this.accumulated > getMaxInMemorySize()) {
					raiseLimitException();
				}
			}
		}

		private void raiseLimitException() {
			// Do not release here, it's likely down via doOnDiscard..
			throw new DataBufferLimitException("Exceeded limit on max bytes to buffer : " + getMaxInMemorySize());
		}
	}

}
