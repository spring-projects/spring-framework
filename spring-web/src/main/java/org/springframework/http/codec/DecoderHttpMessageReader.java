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

package org.springframework.http.codec;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;

/**
 * Implementation of the {@link HttpMessageReader} interface that delegates
 * to a {@link Decoder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DecoderHttpMessageReader<T> implements HttpMessageReader<T> {

	private final Decoder<T> decoder;

	private final List<MediaType> readableMediaTypes;


	/**
	 * Create a {@code CodecHttpMessageConverter} with the given {@link Decoder}.
	 * @param decoder the decoder to use
	 */
	public DecoderHttpMessageReader(Decoder<T> decoder) {
		this.decoder = decoder;
		this.readableMediaTypes = (decoder != null ?
				MediaType.asMediaTypes(decoder.getDecodableMimeTypes()) : Collections.emptyList());
	}


	@Override
	public boolean canRead(ResolvableType type, MediaType mediaType) {
		return this.decoder != null && this.decoder.canDecode(type, mediaType);
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return this.readableMediaTypes;
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

}
