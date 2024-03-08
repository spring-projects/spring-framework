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

import java.util.Collections;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * JSON Path {@link MappingProvider} implementation using {@link Encoder}
 * and {@link Decoder}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 6.2
 */
final class EncoderDecoderMappingProvider implements MappingProvider {

	private final Encoder<?> encoder;

	private final Decoder<?> decoder;

	/**
	 * Create an instance with the specified writers and readers.
	 */
	public EncoderDecoderMappingProvider(Encoder<?> encoder, Decoder<?> decoder) {
		this.encoder = encoder;
		this.decoder = decoder;
	}


	@Nullable
	@Override
	public <T> T map(Object source, Class<T> targetType, Configuration configuration) {
		return mapToTargetType(source, ResolvableType.forClass(targetType));
	}

	@Nullable
	@Override
	public <T> T map(Object source, TypeRef<T> targetType, Configuration configuration) {
		return mapToTargetType(source, ResolvableType.forType(targetType.getType()));
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> T mapToTargetType(Object source, ResolvableType targetType) {
		DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
		MimeType mimeType = MimeTypeUtils.APPLICATION_JSON;
		Map<String, Object> hints = Collections.emptyMap();

		DataBuffer buffer = ((Encoder<T>) this.encoder).encodeValue(
				(T) source, bufferFactory, ResolvableType.forInstance(source), mimeType, hints);

		return ((Decoder<T>) this.decoder).decode(buffer, targetType, mimeType, hints);
	}

}
