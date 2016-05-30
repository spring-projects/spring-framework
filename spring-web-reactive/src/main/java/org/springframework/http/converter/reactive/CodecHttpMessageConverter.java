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
import java.util.stream.Collectors;

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
 */
public class CodecHttpMessageConverter<T> implements HttpMessageConverter<T> {

	private final Encoder<T> encoder;

	private final Decoder<T> decoder;

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
		return this.decoder != null ? this.decoder.getSupportedMimeTypes().stream().
				map(MediaTypeUtils::toMediaType).
				collect(Collectors.toList()) : Collections.emptyList();
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.encoder != null ? this.encoder.getSupportedMimeTypes().stream().
				map(MediaTypeUtils::toMediaType).
				collect(Collectors.toList()) : Collections.emptyList();
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
			headers.setContentType(contentType);
		}
		DataBufferFactory dataBufferFactory = outputMessage.bufferFactory();
		Flux<DataBuffer> body =
				this.encoder.encode(inputStream, dataBufferFactory, type, contentType);
		return outputMessage.writeWith(body);
	}
}
