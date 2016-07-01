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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;


/**
 * Decode a byte stream into JSON and convert to Object's with Jackson.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 *
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractDecoder<Object> {

	private static final MimeType[] MIME_TYPES = new MimeType[] {
			new MimeType("application", "json", StandardCharsets.UTF_8),
			new MimeType("application", "*+json", StandardCharsets.UTF_8)
	};


	private final ObjectMapper mapper;

	private final JsonObjectDecoder fluxObjectDecoder = new JsonObjectDecoder(true);

	private final JsonObjectDecoder monoObjectDecoder = new JsonObjectDecoder(false);


	public JacksonJsonDecoder() {
		this(new ObjectMapper());
	}

	public JacksonJsonDecoder(ObjectMapper mapper) {
		super(MIME_TYPES);
		this.mapper = mapper;
	}


	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		JsonObjectDecoder objectDecoder = this.fluxObjectDecoder;
		return decodeInternal(objectDecoder, inputStream, elementType, mimeType, hints);
	}

	@Override
	public Mono<Object> decodeOne(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		JsonObjectDecoder objectDecoder = this.monoObjectDecoder;
		return decodeInternal(objectDecoder, inputStream, elementType, mimeType, hints).single();
	}

	private Flux<Object> decodeInternal(JsonObjectDecoder objectDecoder, Publisher<DataBuffer> inputStream,
			ResolvableType elementType, MimeType mimeType, Object[] hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		TypeFactory typeFactory = this.mapper.getTypeFactory();
		JavaType javaType = typeFactory.constructType(elementType.getType());

		ObjectReader reader = this.mapper.readerFor(javaType);

		return objectDecoder.decode(inputStream, elementType, mimeType, hints)
				.map(dataBuffer -> {
					try {
						Object value = reader.readValue(dataBuffer.asInputStream());
						DataBufferUtils.release(dataBuffer);
						return value;
					}
					catch (IOException e) {
						return Flux.error(new CodecException("Error while reading the data", e));
					}
				});
	}

}
