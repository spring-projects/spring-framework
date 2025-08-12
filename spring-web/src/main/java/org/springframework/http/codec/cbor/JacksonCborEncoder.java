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

package org.springframework.http.codec.cbor;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of CBOR objects using Jackson 3.x.
 *
 * <p>Stream encoding is currently not supported.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonCborDecoder
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/20513">Add CBOR support to WebFlux</a>
 */
public class JacksonCborEncoder extends AbstractJacksonEncoder<CBORMapper> {

	/**
	 * Construct a new instance with a {@link CBORMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonCborEncoder() {
		super(CBORMapper.builder(), MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper}.
	 * @see CBORMapper#builder()
	 * @see MapperBuilder#findAndAddModules(ClassLoader)
	 */
	public JacksonCborEncoder(CBORMapper mapper) {
		super(mapper, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper} and {@link MimeType}s.
	 * @see CBORMapper#builder()
	 * @see MapperBuilder#findAndAddModules(ClassLoader)
	 */
	public JacksonCborEncoder(CBORMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		throw new UnsupportedOperationException("Stream encoding is currently not supported");
	}

}
