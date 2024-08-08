/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 * @param <T> the type of objects in the input stream
 */
public class EncoderHttpMessageWriter<T> implements HttpMessageWriter<T> {

	private static final Log logger = HttpLogging.forLogName(EncoderHttpMessageWriter.class);


	private final Encoder<T> encoder;

	private final List<MediaType> mediaTypes;

	@Nullable
	private final MediaType defaultMediaType;


	/**
	 * Create an instance wrapping the given {@link Encoder}.
	 */
	public EncoderHttpMessageWriter(Encoder<T> encoder) {
		Assert.notNull(encoder, "Encoder is required");
		initLogger(encoder);
		this.encoder = encoder;
		this.mediaTypes = MediaType.asMediaTypes(encoder.getEncodableMimeTypes());
		this.defaultMediaType = initDefaultMediaType(this.mediaTypes);
	}

	private static void initLogger(Encoder<?> encoder) {
		if (encoder instanceof AbstractEncoder<?> abstractEncoder &&
				encoder.getClass().getName().startsWith("org.springframework.core.codec")) {
			Log logger = HttpLogging.forLog(abstractEncoder.getLogger());
			abstractEncoder.setLogger(logger);
		}
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
	public List<MediaType> getWritableMediaTypes(ResolvableType elementType) {
		return MediaType.asMediaTypes(getEncoder().getEncodableMimeTypes(elementType));
	}

	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		MediaType contentType = updateContentType(message, mediaType);

		Flux<DataBuffer> body = this.encoder.encode(
				inputStream, message.bufferFactory(), elementType, contentType, hints);

		if (inputStream instanceof Mono) {
			return body
					.singleOrEmpty()
					.switchIfEmpty(Mono.defer(() -> {
						message.getHeaders().setContentType(null);
						message.getHeaders().setContentLength(0);
						return message.setComplete().then(Mono.empty());
					}))
					.flatMap(buffer -> {
						Hints.touchDataBuffer(buffer, hints, logger);
						message.getHeaders().setContentLength(buffer.readableByteCount());
						return message.writeWith(Mono.just(buffer)
								.doOnDiscard(DataBuffer.class, DataBufferUtils::release));
					})
					.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
		}

		if (isStreamingMediaType(contentType)) {
			return message.writeAndFlushWith(body.map(buffer -> {
				Hints.touchDataBuffer(buffer, hints, logger);
				return Mono.just(buffer).doOnDiscard(DataBuffer.class, DataBufferUtils::release);
			}));
		}

		if (logger.isDebugEnabled()) {
			body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
		}
		return message.writeWith(body);
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

	private boolean isStreamingMediaType(@Nullable MediaType mediaType) {
		if (mediaType == null || !(this.encoder instanceof HttpMessageEncoder<?> httpMessageEncoder)) {
			return false;
		}
		for (MediaType streamingMediaType : httpMessageEncoder.getStreamingMediaTypes()) {
			if (mediaType.isCompatibleWith(streamingMediaType) && matchParameters(mediaType, streamingMediaType)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchParameters(MediaType streamingMediaType, MediaType mediaType) {
		for (String name : streamingMediaType.getParameters().keySet()) {
			String s1 = streamingMediaType.getParameter(name);
			String s2 = mediaType.getParameter(name);
			if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
				return false;
			}
		}
		return true;
	}


	// Server side only...

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		Map<String, Object> allHints = Hints.merge(hints,
				getWriteHints(actualType, elementType, mediaType, request, response));

		return write(inputStream, elementType, mediaType, response, allHints);
	}

	/**
	 * Get additional hints for encoding for example based on the server request
	 * or annotations from controller method parameters. By default, delegate to
	 * the encoder if it is an instance of {@link HttpMessageEncoder}.
	 */
	protected Map<String, Object> getWriteHints(ResolvableType streamType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		if (this.encoder instanceof HttpMessageEncoder<?> httpMessageEncoder) {
			return httpMessageEncoder.getEncodeHints(streamType, elementType, mediaType, request, response);
		}
		return Hints.none();
	}

}
