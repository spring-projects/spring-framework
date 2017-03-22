/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.Assert;

import static java.util.stream.Collectors.joining;

/**
 * Reader that supports a stream of {@link ServerSentEvent}s and also plain
 * {@link Object}s which is the same as an {@link ServerSentEvent} with data
 * only.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {

	private static final IntPredicate NEWLINE_DELIMITER = b -> b == '\n' || b == '\r';

	private static final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();

	private static final StringDecoder stringDecoder = new StringDecoder(false);


	private final Decoder<?> decoder;


	/**
	 * Constructor with JSON {@code Encoder} for encoding objects.
	 */
	public ServerSentEventHttpMessageReader(Decoder<?> decoder) {
		Assert.notNull(decoder, "Decoder must not be null");
		this.decoder = decoder;
	}


	/**
	 * Return the configured {@code Decoder}.
	 */
	public Decoder<?> getDecoder() {
		return this.decoder;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
	}

	@Override
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType) ||
				ServerSentEvent.class.isAssignableFrom(elementType.getRawClass());
	}


	@Override
	public Flux<Object> read(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		boolean shouldWrap = ServerSentEvent.class.isAssignableFrom(elementType.getRawClass());
		ResolvableType valueType = shouldWrap ? elementType.getGeneric(0) : elementType;

		return Flux.from(message.getBody())
				.concatMap(ServerSentEventHttpMessageReader::splitOnNewline)
				.map(buffer -> {
					CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
					DataBufferUtils.release(buffer);
					return charBuffer.toString();
				})
				.bufferUntil(line -> line.equals("\n"))
				.concatMap(rawLines -> {
					String[] lines = rawLines.stream().collect(joining()).split("\\r?\\n");
					ServerSentEvent<Object> event = buildEvent(lines, valueType, hints);
					return (shouldWrap ? Mono.just(event) : Mono.justOrEmpty(event.data()));
				})
				.cast(Object.class);
	}

	private static Flux<DataBuffer> splitOnNewline(DataBuffer dataBuffer) {
		List<DataBuffer> results = new ArrayList<>();
		int startIdx = 0;
		int endIdx;
		final int limit = dataBuffer.readableByteCount();
		do {
			endIdx = dataBuffer.indexOf(NEWLINE_DELIMITER, startIdx);
			int length = endIdx != -1 ? endIdx - startIdx + 1 : limit - startIdx;
			DataBuffer token = dataBuffer.slice(startIdx, length);
			results.add(DataBufferUtils.retain(token));
			startIdx = endIdx + 1;
		}
		while (startIdx < limit && endIdx != -1);
		DataBufferUtils.release(dataBuffer);
		return Flux.fromIterable(results);
	}

	private ServerSentEvent<Object> buildEvent(String[] lines, ResolvableType valueType,
			Map<String, Object> hints) {

		ServerSentEvent.Builder<Object> sseBuilder = ServerSentEvent.builder();
		StringBuilder mutableData = new StringBuilder();
		StringBuilder mutableComment = new StringBuilder();

		for (String line : lines) {
			if (line.startsWith("id:")) {
				sseBuilder.id(line.substring(3));
			}
			else if (line.startsWith("event:")) {
				sseBuilder.event(line.substring(6));
			}
			else if (line.startsWith("data:")) {
				mutableData.append(line.substring(5)).append("\n");
			}
			else if (line.startsWith("retry:")) {
				sseBuilder.retry(Duration.ofMillis(Long.valueOf(line.substring(6))));
			}
			else if (line.startsWith(":")) {
				mutableComment.append(line.substring(1)).append("\n");
			}
		}
		if (mutableData.length() > 0) {
			String data = mutableData.toString();
			sseBuilder.data(decodeData(data, valueType, hints));
		}
		if (mutableComment.length() > 0) {
			String comment = mutableComment.toString();
			sseBuilder.comment(comment.substring(0, comment.length() - 1));
		}
		return sseBuilder.build();
	}

	private Object decodeData(String data, ResolvableType dataType, Map<String, Object> hints) {

		if (String.class.isAssignableFrom(dataType.getRawClass())) {
			return data.substring(0, data.length() - 1);
		}

		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		Mono<DataBuffer> input = Mono.just(bufferFactory.wrap(bytes));

		return this.decoder
				.decodeToMono(input, dataType, MediaType.TEXT_EVENT_STREAM, hints)
				.block(Duration.ZERO);
	}

	@Override
	public Mono<Object> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		// For single String give StringDecoder a chance which comes after SSE in the order

		if (String.class.equals(elementType.getRawClass())) {
			Flux<DataBuffer> body = message.getBody();
			return stringDecoder.decodeToMono(body, elementType, null, null).cast(Object.class);
		}

		return Mono.error(new UnsupportedOperationException(
				"ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
	}

}
