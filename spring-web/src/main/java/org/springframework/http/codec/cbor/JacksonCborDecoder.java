/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.cbor.CBORMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonDecoder;
import org.springframework.util.MimeType;

/**
 * Decode bytes into CBOR and convert to Objects with Jackson 3.x.
 *
 * <p><a href="https://github.com/FasterXML/jackson-dataformats-binary/issues/110">Stream decoding is currently not supported</a>.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonCborEncoder
 */
public class JacksonCborDecoder extends AbstractJacksonDecoder<CBORMapper> {

	/**
	 * Construct a new instance with a {@link CBORMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonCborDecoder() {
		super(CBORMapper.builder(), MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborDecoder(CBORMapper.Builder builder) {
		super(builder, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper}.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborDecoder(CBORMapper mapper) {
		super(mapper, MediaType.APPLICATION_CBOR);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborDecoder(CBORMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link CBORMapper} and {@link MimeType}s.
	 * @see CBORMapper#builder()
	 */
	public JacksonCborDecoder(CBORMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		throw new UnsupportedOperationException("Stream decoding is currently not supported");
	}

}
