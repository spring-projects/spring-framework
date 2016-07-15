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

/**
 * Implementation of the {@link HttpMessageConverter} interface that delegates to
 * {@link Encoder} and {@link Decoder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
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
				MediaType.toMediaTypes(decoder.getDecodableMimeTypes()) :
				Collections.emptyList();
		this.writableMediaTypes = encoder != null ?
				MediaType.toMediaTypes(encoder.getEncodableMimeTypes()) :
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
		MediaType contentType = getContentType(inputMessage);
		return this.decoder.decode(inputMessage.getBody(), type, contentType);
	}

	@Override
	public Mono<T> readMono(ResolvableType type, ReactiveHttpInputMessage inputMessage) {
		if (this.decoder == null) {
			return Mono.error(new IllegalStateException("No decoder set"));
		}
		MediaType contentType = getContentType(inputMessage);
		return this.decoder.decodeToMono(inputMessage.getBody(), type, contentType);
	}

	private MediaType getContentType(ReactiveHttpInputMessage inputMessage) {
		MediaType contentType = inputMessage.getHeaders().getContentType();
		return (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
	}


	@Override
	public Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType type,
			MediaType contentType, ReactiveHttpOutputMessage outputMessage) {

		if (this.encoder == null) {
			return Mono.error(new IllegalStateException("No decoder set"));
		}

		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			MediaType contentTypeToUse = contentType;
			if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
				contentTypeToUse = getDefaultContentType(type);
			}
			else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
				MediaType mediaType = getDefaultContentType(type);
				contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
			}
			if (contentTypeToUse != null) {
				if (contentTypeToUse.getCharset() == null) {
					MediaType mediaType = getDefaultContentType(type);
					if (mediaType != null && mediaType.getCharset() != null) {
						contentTypeToUse = new MediaType(contentTypeToUse, mediaType.getCharset());
					}
				}
				headers.setContentType(contentTypeToUse);
			}
		}

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();
		Flux<DataBuffer> body = this.encoder.encode(inputStream, bufferFactory, type, contentType);
		return outputMessage.writeWith(body);
	}

	/**
	 * Return the default content type for the given {@code ResolvableType}.
	 * Used when {@link #write} is called without a concrete content type.
	 *
	 * <p>By default returns the first of {@link Encoder#getEncodableMimeTypes()
	 * encodableMimeTypes}, if any.
	 *
	 * @param elementType the type of element for encoding
	 * @return the content type, or {@code null}
	 */
	protected MediaType getDefaultContentType(ResolvableType elementType) {
		return (!this.writableMediaTypes.isEmpty() ? this.writableMediaTypes.get(0) : null);
	}

}
