/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.util.Assert;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://google.github.io/gson/">Google Gson</a>.
 *
 * @author Brian Clozel
 * @since 7.0
 */
public class GsonEncoder extends AbstractEncoder<Object> implements HttpMessageEncoder<Object> {

	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	private static final byte[] EMPTY_BYTES = new byte[0];

	private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
			MediaType.APPLICATION_NDJSON
	};

	private final Gson gson;

	private final List<MediaType> streamingMediaTypes = new ArrayList<>(1);

	/**
	 * Construct a new encoder using a default {@link Gson} instance
	 * and the {@code "application/json"} and {@code "application/*+json"}
	 * MIME types. The {@code "application/x-ndjson"} is configured for streaming.
	 */
	public GsonEncoder() {
		this(new Gson(), DEFAULT_JSON_MIME_TYPES);
		setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
	}

	/**
	 * Construct a new encoder using the given {@link Gson} instance
	 * and the provided MIME types. Use {@link #setStreamingMediaTypes(List)}
	 * for configuring streaming media types.
	 * @param gson the gson instance to use
	 * @param mimeTypes the mime types the decoder should support
	 */
	public GsonEncoder(Gson gson, MimeType... mimeTypes) {
		super(mimeTypes);
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	/**
	 * Configure "streaming" media types for which flushing should be performed
	 * automatically vs at the end of the stream.
	 */
	public void setStreamingMediaTypes(List<MediaType> mediaTypes) {
		this.streamingMediaTypes.clear();
		this.streamingMediaTypes.addAll(mediaTypes);
	}

	@Override
	public List<MediaType> getStreamingMediaTypes() {
		return Collections.unmodifiableList(this.streamingMediaTypes);
	}

	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (!super.canEncode(elementType, mimeType)) {
			return false;
		}
		Class<?> clazz = elementType.toClass();
		return !String.class.isAssignableFrom(elementType.resolve(clazz));
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
					@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		boolean isStreaming = isStreamingMediaType(mimeType);
		if (isStreaming) {
			return Flux.from(inputStream).map(message -> encodeValue(message, bufferFactory, EMPTY_BYTES, NEWLINE_SEPARATOR));
		}
		else {
			JsonArrayJoinHelper helper = new JsonArrayJoinHelper();
			// Do not prepend JSON array prefix until first signal is known, onNext vs onError
			// Keeps response not committed for error handling
			return Flux.from(inputStream)
					.map(value -> {
						byte[] prefix = helper.getPrefix();
						byte[] delimiter = helper.getDelimiter();
						DataBuffer dataBuffer = encodeValue(value, bufferFactory, delimiter, EMPTY_BYTES);
						return (prefix.length > 0 ?
								bufferFactory.join(List.of(bufferFactory.wrap(prefix), dataBuffer)) :
								dataBuffer);
					})
					.switchIfEmpty(Mono.fromCallable(() -> bufferFactory.wrap(helper.getPrefix())))
					.concatWith(Mono.fromCallable(() -> bufferFactory.wrap(helper.getSuffix())));
		}
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory, ResolvableType valueType,
					@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		return encodeValue(value, bufferFactory, EMPTY_BYTES, EMPTY_BYTES);
	}

	private DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
					byte[] prefix, byte[] suffix) {
		try {
			FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
			bos.write(prefix);
			this.gson.toJson(value, writer);
			writer.flush();
			bos.write(suffix);
			byte[] bytes = bos.toByteArrayUnsafe();
			return bufferFactory.wrap(bytes);
		}
		catch (IOException ex) {
			throw new EncodingException("JSON encoding error: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Return the separator to use for the given mime type.
	 * <p>By default, this method returns new line {@code "\n"} if the given
	 * mime type is one of the configured {@link #setStreamingMediaTypes(List)
	 * streaming} mime types.
	 */
	protected boolean isStreamingMediaType(@Nullable MimeType mimeType) {
		for (MediaType streamingMediaType : this.streamingMediaTypes) {
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				return true;
			}
		}
		return false;
	}


	private static class JsonArrayJoinHelper {

		private static final byte[] COMMA_SEPARATOR = {','};

		private static final byte[] OPEN_BRACKET = {'['};

		private static final byte[] CLOSE_BRACKET = {']'};

		private boolean firstItemEmitted;

		public byte[] getDelimiter() {
			if (this.firstItemEmitted) {
				return COMMA_SEPARATOR;
			}
			this.firstItemEmitted = true;
			return EMPTY_BYTES;
		}

		public byte[] getPrefix() {
			return (this.firstItemEmitted ? EMPTY_BYTES : OPEN_BRACKET);
		}

		public byte[] getSuffix() {
			return CLOSE_BRACKET;
		}
	}

}
