/*
 * Copyright 2002-2015 the original author or authors.
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
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.Encoder;
import org.springframework.util.BufferOutputStream;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects.
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonDecoder
 */
public class JacksonJsonEncoder extends AbstractEncoder<Object> {

	private final ObjectMapper mapper;

	private Encoder<ByteBuffer> postProcessor;


	public JacksonJsonEncoder() {
		this(new ObjectMapper(), null);
	}

	public JacksonJsonEncoder(Encoder<ByteBuffer> postProcessor) {
		this(new ObjectMapper(), postProcessor);
	}


	public JacksonJsonEncoder(ObjectMapper mapper, Encoder<ByteBuffer> postProcessor) {
		super(new MimeType("application", "json", StandardCharsets.UTF_8),
				new MimeType("application", "*+json", StandardCharsets.UTF_8));
		this.mapper = mapper;
		this.postProcessor = postProcessor;
	}

	@Override
	public Flux<ByteBuffer> encode(Publisher<? extends Object> inputStream,
			ResolvableType type, MimeType mimeType, Object... hints) {

		Flux<ByteBuffer> stream = Flux.from(inputStream).map(value -> {
			Buffer buffer = new Buffer();
			BufferOutputStream outputStream = new BufferOutputStream(buffer);
			try {
				this.mapper.writeValue(outputStream, value);
			}
			catch (IOException e) {
				throw new CodecException("Error while writing the data", e);
			}
			buffer.flip();
			return buffer.byteBuffer();
		});
		if (this.postProcessor != null) {
			stream = this.postProcessor.encode(stream, type, mimeType, hints);
		};
		return stream;
	}

}
