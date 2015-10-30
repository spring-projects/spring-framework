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

package org.springframework.reactive.codec.decoder;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.reactivestreams.Publisher;
import reactor.Publishers;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.CodecException;
import org.springframework.reactive.codec.encoder.JacksonJsonEncoder;
import org.springframework.reactive.io.ByteBufferInputStream;

/**
 * Decode from a bytes stream of JSON objects to a stream of {@code Object} (POJO).
 *
 * @author Sebastien Deleuze
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder implements ByteToMessageDecoder<Object> {

	private final ObjectMapper mapper;


	public JacksonJsonDecoder() {
		this(new ObjectMapper());
	}

	public JacksonJsonDecoder(ObjectMapper mapper) {
		this.mapper = mapper;
	}


	@Override
	public boolean canDecode(ResolvableType type, MediaType mediaType, Object... hints) {
		return mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
	}

	@Override
	public Publisher<Object> decode(Publisher<ByteBuffer> inputStream, ResolvableType type,
			MediaType mediaType, Object... hints) {

		ObjectReader reader = this.mapper.readerFor(type.getRawClass());
		return Publishers.map(inputStream, chunk -> {
			try {
				return reader.readValue(new ByteBufferInputStream(chunk));
			}
			catch (IOException e) {
				throw new CodecException("Error while reading the data", e);
			}
		});
	}

}
