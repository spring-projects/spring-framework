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

package org.springframework.http.codec;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

/**
 * Reader that supports a stream of {@link ServerSentEvent}s and also plain
 * {@link Object}s which is the same as an {@link ServerSentEvent} with data
 * only.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ServerSentEventHttpMessageReader implements HttpMessageReader<Object> {

	private static final IntPredicate NEWLINE_DELIMITER = b -> b == '\n' || b == '\r';

	private final List<Decoder<?>> dataDecoders;


	public ServerSentEventHttpMessageReader() {
		this.dataDecoders = Collections.emptyList();
	}

	public ServerSentEventHttpMessageReader(List<Decoder<?>> dataDecoders) {
		Assert.notNull(dataDecoders, "'dataDecoders' must not be null");
		this.dataDecoders = new ArrayList<>(dataDecoders);
	}


	@Override
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType) ||
				ServerSentEvent.class.isAssignableFrom(elementType.getRawClass());
	}

	@Override
	public Flux<Object> read(ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {
		boolean isSseElementType = ServerSentEvent.class.isAssignableFrom(elementType.getRawClass());
		ResolvableType dataType = (isSseElementType ? elementType.getGeneric(0) : elementType);
		return Flux.from(inputMessage.getBody())
				.concatMap(ServerSentEventHttpMessageReader::splitOnNewline)
				.map(buffer -> Tuples.of(decodeDataBuffer(buffer), buffer.factory()))
				.bufferUntil(data -> data.getT1().equals("\n"))
				.concatMap(list -> {
					ServerSentEvent.Builder<Object> sseBuilder = ServerSentEvent.builder();
					StringBuilder dataBuilder = new StringBuilder();
					StringBuilder commentBuilder = new StringBuilder();
					DataBufferFactory bufferFactory = list.stream().findFirst().get().getT2();
					String[] lines = list.stream().map(t -> t.getT1()).collect(Collectors.joining()).split("\\r?\\n");
					for (String line : lines) {
						if (line.startsWith("id:")) {
							sseBuilder.id(line.substring(3));
						}
						else if (line.startsWith("event:")) {
							sseBuilder.event(line.substring(6));
						}
						else if (line.startsWith("data:")) {
							dataBuilder.append(line.substring(5)).append("\n");
						}
						else if (line.startsWith("retry:")) {
							sseBuilder.retry(Duration.ofMillis(Long.valueOf(line.substring(6))));
						}
						else if (line.startsWith(":")) {
							commentBuilder.append(line.substring(1)).append("\n");
						}
					}
					if (dataBuilder.length() > 0) {
						String data = dataBuilder.toString();
						if (String.class.isAssignableFrom(dataType.getRawClass())) {
							sseBuilder.data(data.substring(0, data.length() - 1));
						}
						else {
							sseBuilder.data(decode(data, bufferFactory, dataType, hints));
						}
					}
					if (commentBuilder.length() > 0) {
						String comment = commentBuilder.toString();
						sseBuilder.comment(comment.substring(0, comment.length() - 1));
					}
					ServerSentEvent<Object> sse = sseBuilder.build();
					return (isSseElementType ? Mono.just(sse) : Mono.justOrEmpty(sse.data()));
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

	private String decodeDataBuffer(DataBuffer dataBuffer) {
		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer());
		DataBufferUtils.release(dataBuffer);
		return charBuffer.toString();
	}

	@SuppressWarnings("unchecked")
	private <T> T decode(String data, DataBufferFactory bufferFactory, ResolvableType elementType, Map<String, Object> hints) {
		Optional<Decoder<?>> decoder = dataDecoders
				.stream()
				.filter(e -> e.canDecode(elementType, MimeTypeUtils.APPLICATION_JSON))
				.findFirst();
		return ((Decoder<T>) decoder.orElseThrow(() -> new CodecException("No suitable decoder found!")))
				.decodeToMono(Mono.just(bufferFactory.wrap(data.getBytes(StandardCharsets.UTF_8))), elementType, MimeTypeUtils.APPLICATION_JSON, hints).block();
	}

	@Override
	public Mono<Object> readMono(ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("ServerSentEventHttpMessageReader only supports reading stream of events as a Flux"));
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.TEXT_EVENT_STREAM);
	}
}
