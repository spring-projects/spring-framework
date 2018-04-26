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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageWriter} that wraps and delegates to an {@link Encoder}.
 *
 * <p>Also a {@code HttpMessageWriter} that pre-resolves encoding hints
 * from the extra information available on the server side such as the request
 * or controller method annotations.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class EncoderHttpMessageWriter<T> implements HttpMessageWriter<T> {

	private final Encoder<T> encoder;

	private final List<MediaType> mediaTypes;

	@Nullable
	private final MediaType defaultMediaType;


	/**
	 * Create an instance wrapping the given {@link Encoder}.
	 */
	public EncoderHttpMessageWriter(Encoder<T> encoder) {
		Assert.notNull(encoder, "Encoder is required");
		this.encoder = encoder;
		this.mediaTypes = MediaType.asMediaTypes(encoder.getEncodableMimeTypes());
		this.defaultMediaType = initDefaultMediaType(this.mediaTypes);
	}

	@Nullable
	private static MediaType initDefaultMediaType(List<MediaType> mediaTypes) {
		return mediaTypes.stream().filter(MediaType::isConcrete).findFirst().orElse(null);
	}


	/**
	 * Return the {@code Encoder} of this writer.
	 */
	public Encoder<T> getEncoder() {
		return this.encoder;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.mediaTypes;
	}


	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		MediaType contentType = updateContentType(message, mediaType);
		HttpHeaders headers = message.getHeaders();

		if (headers.getContentLength() < 0 && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
			if (inputStream instanceof Mono) {
				// This works because we don't actually commit until after the first signal...
				inputStream = ((Mono<T>) inputStream).doOnNext(data -> {
					Long contentLength = this.encoder.getContentLength(data, contentType);
					if (contentLength != null) {
						headers.setContentLength(contentLength);
					}
				});
			}
		}

		Flux<DataBuffer> body = this.encoder.encode(
				inputStream, message.bufferFactory(), elementType, contentType, hints);

		return (isStreamingMediaType(contentType) ?
				message.writeAndFlushWith(body.map(Flux::just)) : message.writeWith(body));
	}

	@Nullable
	private MediaType updateContentType(ReactiveHttpOutputMessage message, @Nullable MediaType mediaType) {
		MediaType result = message.getHeaders().getContentType();
		if (result != null) {
			return result;
		}
		MediaType fallback = this.defaultMediaType;
		result = (useFallback(mediaType, fallback) ? fallback : mediaType);
		if (result != null) {
			result = addDefaultCharset(result, fallback);
			message.getHeaders().setContentType(result);
		}
		return result;
	}

	private static boolean useFallback(@Nullable MediaType main, @Nullable MediaType fallback) {
		return (main == null || !main.isConcrete() ||
				main.equals(MediaType.APPLICATION_OCTET_STREAM) && fallback != null);
	}

	private static MediaType addDefaultCharset(MediaType main, @Nullable MediaType defaultType) {
		if (main.getCharset() == null && defaultType != null && defaultType.getCharset() != null) {
			return new MediaType(main, defaultType.getCharset());
		}
		return main;
	}

	private boolean isStreamingMediaType(@Nullable MediaType contentType) {
		return (contentType != null && this.encoder instanceof HttpMessageEncoder &&
				((HttpMessageEncoder<?>) this.encoder).getStreamingMediaTypes().stream()
						.anyMatch(contentType::isCompatibleWith));
	}


	// Server side only...

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = new HashMap<>();
		allHints.putAll(getWriteHints(actualType, elementType, mediaType, request, response));
		allHints.putAll(hints);

		return write(inputStream, elementType, mediaType, response, allHints);
	}

	/**
	 * Get additional hints for encoding for example based on the server request
	 * or annotations from controller method parameters. By default, delegate to
	 * the encoder if it is an instance of {@link HttpMessageEncoder}.
	 */
	protected Map<String, Object> getWriteHints(ResolvableType streamType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.encoder instanceof HttpMessageEncoder) {
			HttpMessageEncoder<?> httpEncoder = (HttpMessageEncoder<?>) this.encoder;
			return httpEncoder.getEncodeHints(streamType, elementType, mediaType, request, response);
		}
		return Collections.emptyMap();
	}

}
