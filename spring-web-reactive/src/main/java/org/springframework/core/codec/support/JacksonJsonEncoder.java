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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects.
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonDecoder
 */
public class JacksonJsonEncoder extends AbstractAllocatingEncoder<Object> {

	private final ObjectMapper mapper;

	private Encoder<DataBuffer> postProcessor;

	public JacksonJsonEncoder(DataBufferAllocator allocator) {
		this(allocator, new ObjectMapper(), null);
	}

	public JacksonJsonEncoder(DataBufferAllocator allocator,
			Encoder<DataBuffer> postProcessor) {
		this(allocator, new ObjectMapper(), postProcessor);
	}

	public JacksonJsonEncoder(DataBufferAllocator allocator, ObjectMapper mapper,
			Encoder<DataBuffer> postProcessor) {
		super(allocator, new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		this.mapper = mapper;
		this.postProcessor = postProcessor;
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends Object> inputStream,
			ResolvableType type, MimeType mimeType, Object... hints) {

		Publisher<DataBuffer> stream = (inputStream instanceof Mono ?
				((Mono<?>)inputStream).map(this::serialize) :
				Flux.from(inputStream).map(this::serialize));
		return (this.postProcessor == null ? Flux.from(stream) : this.postProcessor.encode(stream, type, mimeType, hints));
	}

	private DataBuffer serialize(Object value) {
		DataBuffer buffer = allocator().allocateBuffer();
		OutputStream outputStream = buffer.asOutputStream();
		try {
			this.mapper.writeValue(outputStream, value);
		}
		catch (IOException e) {
			throw new CodecException("Error while writing the data", e);
		}
		return buffer;
	}

}
