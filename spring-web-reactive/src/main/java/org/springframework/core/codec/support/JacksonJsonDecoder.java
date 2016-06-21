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

package org.springframework.core.codec.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;


/**
 * Decode from a bytes stream of JSON objects to a stream of {@code Object} (POJO).
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractDecoder<Object> {

	private final ObjectMapper mapper;

	private Decoder<DataBuffer> preProcessor;


	public JacksonJsonDecoder() {
		this(new ObjectMapper(), null);
	}

	public JacksonJsonDecoder(Decoder<DataBuffer> preProcessor) {
		this(new ObjectMapper(), preProcessor);
	}

	public JacksonJsonDecoder(ObjectMapper mapper, Decoder<DataBuffer> preProcessor) {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		this.mapper = mapper;
		this.preProcessor = preProcessor;
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			MimeType mimeType, Object... hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");
		TypeFactory typeFactory = this.mapper.getTypeFactory();
		JavaType javaType = typeFactory.constructType(elementType.getType());
		ObjectReader reader = this.mapper.readerFor(javaType);

		Flux<DataBuffer> stream = Flux.from(inputStream);
		if (this.preProcessor != null) {
			stream = this.preProcessor.decode(inputStream, elementType, mimeType, hints);
		}

		return stream.map(dataBuffer -> {
			try {
				Object value = reader.readValue(dataBuffer.asInputStream());
				DataBufferUtils.release(dataBuffer);
				return value;
			}
			catch (IOException e) {
				throw new CodecException("Error while reading the data", e);
			}
		});
	}

}
