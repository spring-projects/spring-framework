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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects,
 * using Jackson 2.6+.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jackson2JsonDecoder
 */
public class Jackson2JsonEncoder extends AbstractJackson2Codec implements Encoder<Object> {

	private static final ByteBuffer START_ARRAY_BUFFER = ByteBuffer.wrap(new byte[]{'['});

	private static final ByteBuffer SEPARATOR_BUFFER = ByteBuffer.wrap(new byte[]{','});

	private static final ByteBuffer END_ARRAY_BUFFER = ByteBuffer.wrap(new byte[]{']'});


	public Jackson2JsonEncoder() {
		super(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonEncoder(ObjectMapper mapper) {
		super(mapper);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {
		if (mimeType == null) {
			return true;
		}
		return JSON_MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType));
	}

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return JSON_MIME_TYPES;
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		if (inputStream instanceof Mono) {
			return Flux.from(inputStream).map(value -> encodeValue(value, bufferFactory, elementType, hints));
		}

		Mono<DataBuffer> startArray = Mono.just(bufferFactory.wrap(START_ARRAY_BUFFER));
		Mono<DataBuffer> endArray = Mono.just(bufferFactory.wrap(END_ARRAY_BUFFER));

		Flux<DataBuffer> array = Flux.from(inputStream)
				.concatMap(value -> {
					DataBuffer arraySeparator = bufferFactory.wrap(SEPARATOR_BUFFER);
					return Flux.just(encodeValue(value, bufferFactory, elementType, hints), arraySeparator);
				});

		return Flux.concat(startArray, array.skipLast(1), endArray);
	}

	private DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
			ResolvableType type, Map<String, Object> hints) {

		TypeFactory typeFactory = this.mapper.getTypeFactory();
		JavaType javaType = typeFactory.constructType(type.getType());
		if (type.isInstance(value)) {
			javaType = getJavaType(type.getType(), null);
		}

		ObjectWriter writer;
		Class<?> jsonView = (Class<?>)hints.get(AbstractJackson2Codec.JSON_VIEW_HINT);
		if (jsonView != null) {
			writer = this.mapper.writerWithView(jsonView);
		}
		else {
			writer = this.mapper.writer();
		}

		if (javaType != null && javaType.isContainerType()) {
			writer = writer.forType(javaType);
		}

		DataBuffer buffer = bufferFactory.allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();
		try {
			writer.writeValue(outputStream, value);
		}
		catch (IOException ex) {
			throw new CodecException("Error while writing the data", ex);
		}

		return buffer;
	}

}
