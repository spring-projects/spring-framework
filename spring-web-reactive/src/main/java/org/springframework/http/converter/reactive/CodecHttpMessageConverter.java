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

package org.springframework.http.converter.reactive;

import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.support.MediaTypeUtils;

/**
 * Implementation of the {@link HttpMessageConverter} interface that delegates to
 * {@link Encoder} and {@link Decoder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public class CodecHttpMessageConverter<T> implements HttpMessageConverter<T> {

	private final Encoder<T> encoder;

	private final Decoder<T> decoder;

	private final List<MediaType> readableMediaTypes;

	private final List<MediaType> writableMediaTypes;

	/**
	 * Create a {@code CodecHttpMessageConverter} with the given {@link Encoder}. When
	 * using this constructor, all read-related methods will in {@code false} or an
	 * {@link IllegalStateException}.
	 * @param encoder the encoder to use
	 */
	public CodecHttpMessageConverter(Encoder<T> encoder) {
		this(encoder, null);
	}

	/**
	 * Create a {@code CodecHttpMessageConverter} with the given {@link Decoder}. When
	 * using this constructor, all write-related methods will in {@code false} or an
	 * {@link IllegalStateException}.
	 * @param decoder the decoder to use
	 */
	public CodecHttpMessageConverter(Decoder<T> decoder) {
		this(null, decoder);
	}

	/**
	 * Create a {@code CodecHttpMessageConverter} with the given {@link Encoder} and
	 * {@link Decoder}.
	 * @param encoder the encoder to use, can be {@code null}
	 * @param decoder the decoder to use, can be {@code null}
	 */
	public CodecHttpMessageConverter(Encoder<T> encoder, Decoder<T> decoder) {
		this.encoder = encoder;
		this.decoder = decoder;

		this.readableMediaTypes = decoder != null ?
				MediaTypeUtils.toMediaTypes(decoder.getDecodableMimeTypes()) :
				Collections.emptyList();
		this.writableMediaTypes = encoder != null ?
				MediaTypeUtils.toMediaTypes(encoder.getEncodableMimeTypes()) :
				Collections.emptyList();
	}

	@Override
	public boolean canRead(ResolvableType type, MediaType mediaType) {
		return this.decoder != null && this.decoder.canDecode(type, mediaType);
	}

	@Override
	public boolean canWrite(ResolvableType type, MediaType mediaType) {
		return this.encoder != null && this.encoder.canEncode(type, mediaType);
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.readableMediaTypes;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.writableMediaTypes;
	}

	@Override
	public Flux<T> read(ResolvableType type, ReactiveHttpInputMessage inputMessage) {
		if (this.decoder == null) {
			return Flux.error(new IllegalStateException("No decoder set"));
		}
		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}

		Flux<DataBuffer> body = inputMessage.getBody();

		return this.decoder.decode(body, type, contentType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType type,
			MediaType contentType,
			ReactiveHttpOutputMessage outputMessage) {
		if (this.encoder == null) {
			return Mono.error(new IllegalStateException("No decoder set"));
		}
		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			MediaType contentTypeToUse = contentType;
			if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
				contentTypeToUse = getDefaultContentType(type);
			}
			headers.setContentType(contentTypeToUse);
		}
		DataBufferFactory dataBufferFactory = outputMessage.bufferFactory();
		Flux<DataBuffer> body =
				this.encoder.encode(inputStream, dataBufferFactory, type, contentType);
		return outputMessage.writeWith(body);
	}

	/**
	 * Returns the default content type for the given type. Called when {@link #write}
	 * is invoked without a specified content type parameter.
	 * <p>By default, this returns a {@link MediaType} created using the first element of
	 * the encoder {@link Encoder#getEncodableMimeTypes() encodableMimeTypes} property, if any.
	 * Can be overridden in subclasses.
	 * @param type the type to return the content type for
	 * @return the content type, or {@code null} if not known
	 */
	protected MediaType getDefaultContentType(ResolvableType type) {
		return (!this.writableMediaTypes.isEmpty() ? this.writableMediaTypes.get(0) : null);
	}
}
