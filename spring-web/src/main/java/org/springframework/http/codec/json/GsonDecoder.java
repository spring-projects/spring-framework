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

import java.io.InputStreamReader;
import java.util.Map;

import com.google.gson.Gson;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDataBufferDecoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link Decoder} that reads a byte stream into JSON and converts it to Objects with
 * <a href="https://google.github.io/gson/">Google Gson</a>.
 * <p>{@code Flux<*>} target types are not available because non-blocking parsing is not supported,
 * so this decoder targets only {@code Mono<*>} types. Attempting to decode to a {@code Flux<*>} will
 * result in a {@link UnsupportedOperationException} being thrown at runtime.
 *
 * @author Brian Clozel
 * @since 7.0
 */
public class GsonDecoder extends AbstractDataBufferDecoder<Object> {

	private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
	};

	private final Gson gson;

	/**
	 * Construct a new decoder using a default {@link Gson} instance
	 * and the {@code "application/json"} and {@code "application/*+json"}
	 * MIME types.
	 */
	public GsonDecoder() {
		this(new Gson(), DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new decoder using the given {@link Gson} instance
	 * and the provided MIME types.
	 * @param gson the gson instance to use
	 * @param mimeTypes the mime types the decoder should support
	 */
	public GsonDecoder(Gson gson, MimeType... mimeTypes) {
		super(mimeTypes);
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		if (!super.canDecode(elementType, mimeType)) {
			return false;
		}
		return !CharSequence.class.isAssignableFrom(elementType.toClass());
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		throw new UnsupportedOperationException("Stream decoding is currently not supported");
	}

	@Override
	public @Nullable Object decode(DataBuffer buffer, ResolvableType targetType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
		try {
			return this.gson.fromJson(new InputStreamReader(buffer.asInputStream()), targetType.getType());
		}
		finally {
			DataBufferUtils.release(buffer);
		}
	}

}
