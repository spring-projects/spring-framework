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

package org.springframework.reactive.codec.encoder;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.io.buffer.Buffer;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.CodecException;
import org.springframework.reactive.codec.decoder.JacksonJsonDecoder;
import org.springframework.reactive.io.BufferOutputStream;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects.
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonDecoder
 */
public class JacksonJsonEncoder implements MessageToByteEncoder<Object> {

	private final ObjectMapper mapper;

	public JacksonJsonEncoder() {
		this(new ObjectMapper());
	}

	public JacksonJsonEncoder(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public boolean canEncode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
	}

	@Override
	public Publisher<ByteBuffer> encode(Publisher<? extends Object> messageStream,
			ResolvableType type, MediaType mediaType, Object... hints) {

		return Publishers.map(messageStream, value -> {
			Buffer buffer = new Buffer();
			BufferOutputStream outputStream = new BufferOutputStream(buffer);
			try {
				this.mapper.writeValue(outputStream, value);
			} catch (IOException e) {
				throw new CodecException("Error while writing the data", e);
			}
			buffer.flip();
			return buffer.byteBuffer();
		});
	}

}
