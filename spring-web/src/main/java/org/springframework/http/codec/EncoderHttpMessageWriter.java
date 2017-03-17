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


	/**
	 * Create an instance wrapping the given {@link Encoder}.
	 */
	public EncoderHttpMessageWriter(Encoder<T> encoder) {
		Assert.notNull(encoder, "Encoder is required");
		this.encoder = encoder;
		this.mediaTypes = MediaType.asMediaTypes(encoder.getEncodableMimeTypes());
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
			MediaType contentTypeToUse = mediaType;
			if (mediaType == null || mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
				contentTypeToUse = getDefaultContentType(elementType);
			}
			else if (MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
				MediaType contentType = getDefaultContentType(elementType);
				contentTypeToUse = (contentType != null ? contentType : contentTypeToUse);
			}
			if (contentTypeToUse != null) {
				if (contentTypeToUse.getCharset() == null) {
					MediaType contentType = getDefaultContentType(elementType);
					if (contentType != null && contentType.getCharset() != null) {
						contentTypeToUse = new MediaType(contentTypeToUse, contentType.getCharset());
					}
				}
				headers.setContentType(contentTypeToUse);
			}
		}

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();
		Flux<DataBuffer> body = this.encoder.encode(inputStream, bufferFactory, elementType, mediaType, hints);
		return (hints.get(FLUSHING_STRATEGY_HINT) == AFTER_EACH_ELEMENT ?
				outputMessage.writeAndFlushWith(body.map(Flux::just)) : outputMessage.writeWith(body));
	}

	/**
	 * Return the default content type for the given {@code ResolvableType}.
	 * Used when {@link #write} is called without a concrete content type.
	 *
	 * <p>By default returns the first of {@link Encoder#getEncodableMimeTypes()
	 * encodableMimeTypes} that is concrete({@link MediaType#isConcrete()}), if any.
	 *
	 * @param elementType the type of element for encoding
	 * @return the content type, or {@code null}
	 */
	protected MediaType getDefaultContentType(ResolvableType elementType) {
		return this.mediaTypes.stream().filter(MediaType::isConcrete).findFirst().orElse(null);
	}

}
