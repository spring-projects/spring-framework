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
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.AbstractJackson2Decoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Decode bytes into CBOR and convert to Object's with Jackson 2.x.
 *
 * <p><a href="https://github.com/FasterXML/jackson-dataformats-binary/issues/110">Stream decoding is currently not supported</a>.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 * @see Jackson2CborEncoder
 * @deprecated since 7.0 in favor of {@link JacksonCborDecoder}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class Jackson2CborDecoder extends AbstractJackson2Decoder {

	public Jackson2CborDecoder() {
		this(Jackson2ObjectMapperBuilder.cbor().build(), MediaType.APPLICATION_CBOR);
	}

	public Jackson2CborDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(CBORFactory.class, mapper.getFactory().getClass());
	}


	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		throw new UnsupportedOperationException("Does not support stream decoding yet");
	}

}
