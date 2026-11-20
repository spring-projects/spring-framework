/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.util.Collections;
import java.util.Map;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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


	@Override
	public <T> @Nullable T map(Object source, Class<T> targetType, Configuration configuration) {
		return mapToTargetType(source, ResolvableType.forClass(targetType));
	}

	@Override
	public <T> @Nullable T map(Object source, TypeRef<T> targetType, Configuration configuration) {
		return mapToTargetType(source, ResolvableType.forType(targetType.getType()));
	}

	@SuppressWarnings("unchecked")
	private <T> @Nullable T mapToTargetType(Object source, ResolvableType targetType) {
		DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;
		MimeType mimeType = MimeTypeUtils.APPLICATION_JSON;
		Map<String, Object> hints = Collections.emptyMap();

		DataBuffer buffer = ((Encoder<T>) this.encoder).encodeValue(
				(T) source, bufferFactory, ResolvableType.forInstance(source), mimeType, hints);

		return ((Decoder<T>) this.decoder).decode(buffer, targetType, mimeType, hints);
	}

}
