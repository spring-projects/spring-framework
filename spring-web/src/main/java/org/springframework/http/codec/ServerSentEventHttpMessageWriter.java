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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

/**
 * Encoder that supports a stream of {@link ServerSentEvent}s and also plain
 * {@link Object}s which is the same as an {@link ServerSentEvent} with data
 * only.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

	private static final MediaType TEXT_EVENT_STREAM =
			new MediaType("text", "event-stream");

	private final List<Encoder<?>> dataEncoders;

	public ServerSentEventHttpMessageWriter() {
		this.dataEncoders = Collections.emptyList();
	}

	public ServerSentEventHttpMessageWriter(List<Encoder<?>> dataEncoders) {
		Assert.notNull(dataEncoders, "'dataEncoders' must not be null");
		this.dataEncoders = dataEncoders;
	}

	@Override
	public boolean canWrite(ResolvableType type, MediaType mediaType) {
		return mediaType == null || TEXT_EVENT_STREAM.isCompatibleWith(mediaType);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return Collections.singletonList(TEXT_EVENT_STREAM);
	}

	@Override
	public Mono<Void> write(Publisher<?> inputStream, ResolvableType type,
							MediaType contentType, ReactiveHttpOutputMessage outputMessage) {

		outputMessage.getHeaders().setContentType(TEXT_EVENT_STREAM);

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();
		Flux<Publisher<DataBuffer>> body = encode(inputStream, bufferFactory, type);

		return outputMessage.writeAndFlushWith(body);
	}

	private Flux<Publisher<DataBuffer>> encode(Publisher<?> inputStream,
											   DataBufferFactory bufferFactory, ResolvableType type) {

		return Flux.from(inputStream)
				.map(o -> toSseEvent(o, type))
				.map(sse -> {
					StringBuilder sb = new StringBuilder();
					sse.id().ifPresent(id -> writeField("id", id, sb));
					sse.event().ifPresent(event -> writeField("event", event, sb));
					sse.retry().ifPresent(retry -> writeField("retry", retry.toMillis(), sb));
					sse.comment().ifPresent(comment -> {
						comment = comment.replaceAll("\\n", "\n:");
						sb.append(':').append(comment).append("\n");
					});
					Flux<DataBuffer> dataBuffer = sse.data()
							.<Flux<DataBuffer>>map(data -> {
								sb.append("data:");
								if (data instanceof String) {
									String stringData = ((String) data).replaceAll("\\n", "\ndata:");
									sb.append(stringData).append('\n');
									return Flux.empty();
								}
								else {
									return applyEncoder(data, bufferFactory);
								}
							}).orElse(Flux.empty());

					return Flux.concat(encodeString(sb.toString(), bufferFactory), dataBuffer,
							encodeString("\n", bufferFactory));
				});

	}

	private ServerSentEvent<?> toSseEvent(Object data, ResolvableType type) {
		return ServerSentEvent.class.isAssignableFrom(type.getRawClass())
				? (ServerSentEvent<?>) data
				: ServerSentEvent.builder().data(data).build();
	}

	private void writeField(String fieldName, Object fieldValue, StringBuilder stringBuilder) {
		stringBuilder.append(fieldName);
		stringBuilder.append(':');
		stringBuilder.append(fieldValue.toString());
		stringBuilder.append("\n");
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> applyEncoder(Object data, DataBufferFactory bufferFactory) {
		ResolvableType elementType = ResolvableType.forClass(data.getClass());
		Optional<Encoder<?>> encoder = dataEncoders
				.stream()
				.filter(e -> e.canEncode(elementType, MimeTypeUtils.APPLICATION_JSON))
				.findFirst();
		return ((Encoder<T>) encoder.orElseThrow(() -> new CodecException("No suitable encoder found!")))
				.encode(Mono.just((T) data), bufferFactory, elementType, MimeTypeUtils.APPLICATION_JSON)
				.concatWith(encodeString("\n", bufferFactory));
	}

	private Mono<DataBuffer> encodeString(String str, DataBufferFactory bufferFactory) {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length).write(bytes);
		return Mono.just(buffer);
	}

}
