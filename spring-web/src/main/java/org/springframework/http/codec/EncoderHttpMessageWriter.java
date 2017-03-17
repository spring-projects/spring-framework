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

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.Assert;

import static org.springframework.core.codec.AbstractEncoder.FLUSHING_STRATEGY_HINT;
import static org.springframework.core.codec.AbstractEncoder.FlushingStrategy.AFTER_EACH_ELEMENT;

/**
 * Implementation of {@code HttpMessageWriter} delegating to an {@link Encoder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class EncoderHttpMessageWriter<T> implements HttpMessageWriter<T> {

	private final Encoder<T> encoder;

	private final List<MediaType> mediaTypes;

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
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		HttpHeaders headers = outputMessage.getHeaders();

		if (headers.getContentType() == null) {
			MediaType fallback = this.defaultMediaType;
			MediaType selected = useFallback(mediaType, fallback) ? fallback : mediaType;
			if (selected != null) {
				selected = addDefaultCharset(selected, fallback);
				headers.setContentType(selected);
			}
		}

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();
		Flux<DataBuffer> body = this.encoder.encode(inputStream, bufferFactory, elementType, mediaType, hints);
		return (hints.get(FLUSHING_STRATEGY_HINT) == AFTER_EACH_ELEMENT ?
				outputMessage.writeAndFlushWith(body.map(Flux::just)) : outputMessage.writeWith(body));
	}


	private static boolean useFallback(MediaType main, MediaType fallback) {
		return main == null || !main.isConcrete() ||
				main.equals(MediaType.APPLICATION_OCTET_STREAM) && fallback != null;
	}

	private static MediaType addDefaultCharset(MediaType main, MediaType defaultType) {
		if (main.getCharset() == null && defaultType != null && defaultType.getCharset() != null) {
			return new MediaType(main, defaultType.getCharset());
		}
		return main;
	}

}
