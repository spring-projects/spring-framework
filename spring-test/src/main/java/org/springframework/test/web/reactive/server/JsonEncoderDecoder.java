/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;

/**
 * {@link Encoder} and {@link Decoder} that is able to handle a map to and from
 * JSON. Used to configure the jsonpath infrastructure without having a hard
 * dependency on the library.
 *
 * @param encoder the JSON encoder
 * @param decoder the JSON decoder
 * @author Stephane Nicoll
 * @author Rossen Stoyanchev
 * @since 6.2
 */
record JsonEncoderDecoder(Encoder<?> encoder, Decoder<?> decoder) {

	private static final ResolvableType MAP_TYPE = ResolvableType.forClass(Map.class);


	/**
	 * Create a {@link JsonEncoderDecoder} instance based on the specified
	 * infrastructure.
	 * @param messageWriters the HTTP message writers
	 * @param messageReaders the HTTP message readers
	 * @return a {@link JsonEncoderDecoder} or {@code null} if a suitable codec
	 * is not available
	 */
	@Nullable
	static JsonEncoderDecoder from(Collection<HttpMessageWriter<?>> messageWriters,
			Collection<HttpMessageReader<?>> messageReaders) {

		Encoder<?> jsonEncoder = findJsonEncoder(messageWriters);
		Decoder<?> jsonDecoder = findJsonDecoder(messageReaders);
		if (jsonEncoder != null && jsonDecoder != null) {
			return new JsonEncoderDecoder(jsonEncoder, jsonDecoder);
		}
		return null;
	}


	/**
	 * Find the first suitable {@link Encoder} that can encode a {@link Map}
	 * to JSON.
	 * @param writers the writers to inspect
	 * @return a suitable JSON {@link Encoder} or {@code null}
	 */
	@Nullable
	private static Encoder<?> findJsonEncoder(Collection<HttpMessageWriter<?>> writers) {
		return findJsonEncoder(writers.stream()
				.filter(EncoderHttpMessageWriter.class::isInstance)
				.map(writer -> ((EncoderHttpMessageWriter<?>) writer).getEncoder()));
	}

	@Nullable
	private static Encoder<?> findJsonEncoder(Stream<Encoder<?>> stream) {
		return stream
				.filter(encoder -> encoder.canEncode(MAP_TYPE, MediaType.APPLICATION_JSON))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Find the first suitable {@link Decoder} that can decode a {@link Map} to
	 * JSON.
	 * @param readers the readers to inspect
	 * @return a suitable JSON {@link Decoder} or {@code null}
	 */
	@Nullable
	private static Decoder<?> findJsonDecoder(Collection<HttpMessageReader<?>> readers) {
		return findJsonDecoder(readers.stream()
				.filter(DecoderHttpMessageReader.class::isInstance)
				.map(reader -> ((DecoderHttpMessageReader<?>) reader).getDecoder()));
	}

	@Nullable
	private static Decoder<?> findJsonDecoder(Stream<Decoder<?>> decoderStream) {
		return decoderStream
				.filter(decoder -> decoder.canDecode(MAP_TYPE, MediaType.APPLICATION_JSON))
				.findFirst()
				.orElse(null);
	}

}
