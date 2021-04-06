/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code HttpMessageWriter} for {@code "text/event-stream"} responses.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServerSentEventHttpMessageWriter implements HttpMessageWriter<Object> {

	private static final MediaType DEFAULT_MEDIA_TYPE = new MediaType("text", "event-stream", StandardCharsets.UTF_8);

	private static final List<MediaType> WRITABLE_MEDIA_TYPES = Collections.singletonList(MediaType.TEXT_EVENT_STREAM);

	private static final Log logger = HttpLogging.forLogName(ServerSentEventHttpMessageWriter.class);


	@Nullable
	private final Encoder<?> encoder;


	/**
	 * Constructor without an {@code Encoder}. In this mode only {@code String}
	 * is supported for event data to be encoded.
	 */
	public ServerSentEventHttpMessageWriter() {
		this(null);
	}

	/**
	 * Constructor with JSON {@code Encoder} for encoding objects.
	 * Support for {@code String} event data is built-in.
	 * @param encoder the Encoder to use (may be {@code null})
	 */
	public ServerSentEventHttpMessageWriter(@Nullable Encoder<?> encoder) {
		this.encoder = encoder;
	}


	/**
	 * Return the configured {@code Encoder}, if any.
	 */
	@Nullable
	public Encoder<?> getEncoder() {
		return this.encoder;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return WRITABLE_MEDIA_TYPES;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return (mediaType == null || MediaType.TEXT_EVENT_STREAM.includes(mediaType) ||
				ServerSentEvent.class.isAssignableFrom(elementType.toClass()));
	}

	@Override
	public Mono<Void> write(Publisher<?> input, ResolvableType elementType, @Nullable MediaType mediaType,
			ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		mediaType = (mediaType != null && mediaType.getCharset() != null ? mediaType : DEFAULT_MEDIA_TYPE);
		DataBufferFactory bufferFactory = message.bufferFactory();

		message.getHeaders().setContentType(mediaType);
		return message.writeAndFlushWith(encode(input, elementType, mediaType, bufferFactory, hints));
	}

	private Flux<Publisher<DataBuffer>> encode(Publisher<?> input, ResolvableType elementType,
			MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

		ResolvableType dataType = (ServerSentEvent.class.isAssignableFrom(elementType.toClass()) ?
				elementType.getGeneric() : elementType);

		return Flux.from(input).map(element -> {

			ServerSentEvent<?> sse = (element instanceof ServerSentEvent ?
					(ServerSentEvent<?>) element : ServerSentEvent.builder().data(element).build());

			StringBuilder sb = new StringBuilder();
			String id = sse.id();
			String event = sse.event();
			Duration retry = sse.retry();
			String comment = sse.comment();
			Object data = sse.data();
			if (id != null) {
				writeField("id", id, sb);
			}
			if (event != null) {
				writeField("event", event, sb);
			}
			if (retry != null) {
				writeField("retry", retry.toMillis(), sb);
			}
			if (comment != null) {
				sb.append(':').append(StringUtils.replace(comment, "\n", "\n:")).append("\n");
			}
			if (data != null) {
				sb.append("data:");
			}

			Flux<DataBuffer> result;
			if (data == null) {
				result = Flux.just(encodeText(sb + "\n", mediaType, factory));
			}
			else if (data instanceof String) {
				data = StringUtils.replace((String) data, "\n", "\ndata:");
				result = Flux.just(encodeText(sb + (String) data + "\n\n", mediaType, factory));
			}
			else {
				result = encodeEvent(sb, data, dataType, mediaType, factory, hints);
			}

			return result.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		});
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodeEvent(StringBuilder eventContent, T data, ResolvableType dataType,
			MediaType mediaType, DataBufferFactory factory, Map<String, Object> hints) {

		if (this.encoder == null) {
			throw new CodecException("No SSE encoder configured and the data is not String.");
		}

		return Flux.defer(() -> {
			DataBuffer startBuffer = encodeText(eventContent, mediaType, factory);
			DataBuffer endBuffer = encodeText("\n\n", mediaType, factory);
			DataBuffer dataBuffer = ((Encoder<T>) this.encoder).encodeValue(data, factory, dataType, mediaType, hints);
			Hints.touchDataBuffer(dataBuffer, hints, logger);
			return Flux.just(startBuffer, dataBuffer, endBuffer);
		});
	}

	private void writeField(String fieldName, Object fieldValue, StringBuilder sb) {
		sb.append(fieldName).append(':').append(fieldValue).append("\n");
	}

	private DataBuffer encodeText(CharSequence text, MediaType mediaType, DataBufferFactory bufferFactory) {
		Assert.notNull(mediaType.getCharset(), "Expected MediaType with charset");
		byte[] bytes = text.toString().getBytes(mediaType.getCharset());
		return bufferFactory.wrap(bytes);  // wrapping, not allocating
	}

	@Override
	public Mono<Void> write(Publisher<?> input, ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response,
			Map<String, Object> hints) {

		Map<String, Object> allHints = Hints.merge(hints,
				getEncodeHints(actualType, elementType, mediaType, request, response));

		return write(input, elementType, mediaType, response, allHints);
	}

	private Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.encoder instanceof HttpMessageEncoder) {
			HttpMessageEncoder<?> encoder = (HttpMessageEncoder<?>) this.encoder;
			return encoder.getEncodeHints(actualType, elementType, mediaType, request, response);
		}
		return Hints.none();
	}

}
