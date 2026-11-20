/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of CBOR objects using Jackson 3.x.
 *
 * <p><a href="https://github.com/FasterXML/jackson-dataformats-binary/issues/110">Stream encoding is currently not supported</a>,
 * but a single value can be encoded, for example as a {@code Mono}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonCborDecoder
 */
public class JacksonCborEncoder extends AbstractJacksonEncoder<CBORMapper> {

	/**
	 * Construct a new instance with a {@link CBORMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonCborEncoder() {
		super(CBORMapper.builder(), MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborEncoder(CBORMapper.Builder builder) {
		super(builder, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborEncoder(CBORMapper mapper) {
		super(mapper, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborEncoder(CBORMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper} and {@link MimeType}s.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborEncoder(CBORMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (inputStream instanceof Mono) {
			return super.encode(inputStream, bufferFactory, elementType, mimeType, hints);
		}
		throw new UnsupportedOperationException("Stream encoding is currently not supported");
	}

}
