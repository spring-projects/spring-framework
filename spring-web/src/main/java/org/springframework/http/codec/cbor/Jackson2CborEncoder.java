/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.cbor;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Encoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of CBOR objects using Jackson 2.x.
 *
 * <p><a href="https://github.com/FasterXML/jackson-dataformats-binary/issues/110">Stream encoding is currently not supported</a>.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 * @see Jackson2CborDecoder
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/20513">Add CBOR support to WebFlux</a>
 * @deprecated since 7.0 in favor of {@link JacksonCborEncoder}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2CborEncoder extends AbstractJackson2Encoder {

	public Jackson2CborEncoder() {
		this(Jackson2ObjectMapperBuilder.cbor().build(), MediaType.APPLICATION_CBOR);
	}

	public Jackson2CborEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
	}


	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		throw new UnsupportedOperationException("Does not support stream encoding yet");
	}

}
