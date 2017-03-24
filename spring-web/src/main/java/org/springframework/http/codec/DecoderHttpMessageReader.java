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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageReader} that wraps and delegates to a {@link Decoder}.
 *
 * <p>Also a {@code HttpMessageReader} that pre-resolves decoding hints
 * from the extra information available on the server side such as the request
 * or controller method parameter annotations.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {

	private final Decoder<T> decoder;

	private final List<MediaType> mediaTypes;


	/**
	 * Create an instance wrapping the given {@link Decoder}.
	 */
	public DecoderHttpMessageReader(Decoder<T> decoder) {
		Assert.notNull(decoder, "Decoder is required");
		this.decoder = decoder;
		this.mediaTypes = MediaType.asMediaTypes(decoder.getDecodableMimeTypes());
	}


	/**
	 * Return the {@link Decoder} of this reader.
	 */
	public Decoder<T> getDecoder() {
		return this.decoder;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.mediaTypes;
	}


	@Override
	public boolean canRead(ResolvableType elementType, MediaType mediaType) {
		return this.decoder.canDecode(elementType, mediaType);
	}

	@Override
	public Flux<T> read(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		MediaType contentType = getContentType(message);
		return this.decoder.decode(message.getBody(), elementType, contentType, hints);
	}

	@Override
	public Mono<T> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		MediaType contentType = getContentType(message);
		return this.decoder.decodeToMono(message.getBody(), elementType, contentType, hints);
	}

	private MediaType getContentType(HttpMessage inputMessage) {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
	}


	// Server-side only...

	@Override
	public Flux<T> read(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = new HashMap<>(4);
		allHints.putAll(getReadHints(actualType, elementType, request, response));
		allHints.putAll(hints);

		return read(elementType, request, allHints);
	}

	@Override
	public Mono<T> readMono(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = new HashMap<>(4);
		allHints.putAll(getReadHints(actualType, elementType, request, response));
		allHints.putAll(hints);

		return readMono(elementType, request, allHints);
	}

	/**
	 * Get additional hints for decoding for example based on the server request
	 * or annotations from controller method parameters. By default, delegate to
	 * the decoder if it is an instance of {@link HttpMessageDecoder}.
	 */
	protected Map<String, Object> getReadHints(ResolvableType streamType,
			ResolvableType elementType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.decoder instanceof HttpMessageDecoder) {
			HttpMessageDecoder<?> httpDecoder = (HttpMessageDecoder<?>) this.decoder;
			return httpDecoder.getDecodeHints(streamType, elementType, request, response);
		}
		return Collections.emptyMap();
	}

}
