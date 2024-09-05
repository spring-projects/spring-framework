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

package org.springframework.http.codec.json;

import java.util.List;
import java.util.Map;

import kotlinx.serialization.json.Json;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.KotlinSerializationStringEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using
 * <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a>.
 *
 * <p>This encoder can be used to bind {@code @Serializable} Kotlin classes,
 * <a href="https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism">open polymorphic serialization</a>
 * is not supported.
 * It supports {@code application/json}, {@code application/x-ndjson} and {@code application/*+json} with
 * various character sets, {@code UTF-8} being the default.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 * @since 5.3
 */
public class KotlinSerializationJsonEncoder extends KotlinSerializationStringEncoder<Json> {

	public KotlinSerializationJsonEncoder() {
		this(Json.Default);
	}

	public KotlinSerializationJsonEncoder(Json json) {
		super(json, MediaType.APPLICATION_JSON, new MediaType("application", "*+json"),
				MediaType.APPLICATION_NDJSON);
		setStreamingMediaTypes(List.of(MediaType.APPLICATION_NDJSON));
	}

	@Override
	public Flux<DataBuffer> encodeNonStream(Publisher<?> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		JsonArrayJoinHelper helper = new JsonArrayJoinHelper();
		return Flux.from(inputStream)
				.map(value -> encodeStreamingValue(value, bufferFactory, elementType, mimeType, hints,
						helper.getPrefix(), EMPTY_BYTES))
				.switchIfEmpty(Mono.fromCallable(() -> bufferFactory.wrap(helper.getPrefix())))
				.concatWith(Mono.fromCallable(() -> bufferFactory.wrap(helper.getSuffix())));
	}


	private static class JsonArrayJoinHelper {

		private static final byte[] COMMA_SEPARATOR = {','};

		private static final byte[] OPEN_BRACKET = {'['};

		private static final byte[] CLOSE_BRACKET = {']'};

		private boolean firstItemEmitted;

		public byte[] getPrefix() {
			byte[] prefix = (this.firstItemEmitted ? COMMA_SEPARATOR : OPEN_BRACKET);
			this.firstItemEmitted = true;
			return prefix;
		}

		public byte[] getSuffix() {
			return CLOSE_BRACKET;
		}
	}

}
