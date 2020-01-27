/*
 * Copyright 2002-2019 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Encoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of CBOR objects using Jackson.
 * Stream encoding is not supported yet.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 * @see Jackson2CborDecoder
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/20513">Add CBOR support to WebFlux</a>
 */
public class Jackson2CborEncoder extends AbstractJackson2Encoder {

	public Jackson2CborEncoder() {
		this(Jackson2ObjectMapperBuilder.cbor().build(), MediaType.APPLICATION_CBOR);
	}

	public Jackson2CborEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
	}


	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
		throw new UnsupportedOperationException("Does not support stream encoding yet");
	}

}
