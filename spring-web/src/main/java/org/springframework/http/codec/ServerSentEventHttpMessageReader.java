/*
 * Copyright 2002-2018 the original author or authors.
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

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

	private static final StringDecoder stringDecoder = StringDecoder.textPlainOnly();

	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);


	@Nullable
	private final Decoder<?> decoder;


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

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return (MediaType.TEXT_EVENT_STREAM.includes(mediaType) || isServerSentEvent(elementType));
	}

	private boolean isServerSentEvent(ResolvableType elementType) {
		Class<?> rawClass = elementType.getRawClass();
		return (rawClass != null && ServerSentEvent.class.isAssignableFrom(rawClass));
	}


	@Override
	public Flux<Object> read(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		boolean shouldWrap = isServerSentEvent(elementType);
		ResolvableType valueType = (shouldWrap ? elementType.getGeneric() : elementType);

		return stringDecoder.decode(message.getBody(), STRING_TYPE, null, hints)
				.bufferUntil(line -> line.equals(""))
				.concatMap(lines -> buildEvent(lines, valueType, shouldWrap, hints));
	}

	private Mono<?> buildEvent(List<String> lines, ResolvableType valueType, boolean shouldWrap,
			Map<String, Object> hints) {

		ServerSentEvent.Builder<Object> sseBuilder = shouldWrap ? ServerSentEvent.builder() : null;
		StringBuilder data = null;
		StringBuilder comment = null;

		for (String line : lines) {
			if (line.startsWith("data:")) {
				data = (data != null ? data : new StringBuilder());
				data.append(line.substring(5)).append("\n");
			}
			if (shouldWrap) {
				if (line.startsWith("id:")) {
					sseBuilder.id(line.substring(3));
				}
				else if (line.startsWith("event:")) {
					sseBuilder.event(line.substring(6));
				}
				else if (line.startsWith("retry:")) {
					sseBuilder.retry(Duration.ofMillis(Long.valueOf(line.substring(6))));
				}
				else if (line.startsWith(":")) {
					comment = (comment != null ? comment : new StringBuilder());
					comment.append(line.substring(1)).append("\n");
				}
			}
		}

		Mono<?> decodedData = (data != null ? decodeData(data.toString(), valueType, hints) : Mono.empty());

		if (shouldWrap) {
			if (comment != null) {
				sseBuilder.comment(comment.toString().substring(0, comment.length() - 1));
			}
			return decodedData.map(o -> {
				sseBuilder.data(o);
				return sseBuilder.build();
			});
		}
		else {
			return decodedData;
		}
	}

	private Mono<?> decodeData(String data, ResolvableType dataType, Map<String, Object> hints) {
		if (String.class == dataType.resolve()) {
			return Mono.just(data.substring(0, data.length() - 1));
		}

		if (this.decoder == null) {
			return Mono.error(new CodecException("No SSE decoder configured and the data is not String."));
		}

		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		Mono<DataBuffer> input = Mono.just(bufferFactory.wrap(bytes));
		return this.decoder.decodeToMono(input, dataType, MediaType.TEXT_EVENT_STREAM, hints);
	}

	@Override
	public Mono<Object> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		// We're ahead of String + "*/*"
		// Let's see if we can aggregate the output (lest we time out)...

		if (String.class.equals(elementType.getRawClass())) {
			Flux<DataBuffer> body = message.getBody();
			return stringDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
		}

		return Mono.error(new UnsupportedOperationException(
				"ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
	}

}
