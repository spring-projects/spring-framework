/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.cbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://cbor.io/">CBOR</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/master/cbor">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_CBOR_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by
 * {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @deprecated since 7.0 in favor of {@link JacksonCborHttpMessageConverter}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class MappingJackson2CborHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * Construct a new {@code MappingJackson2CborHttpMessageConverter} using the
	 * default configuration provided by {@code Jackson2ObjectMapperBuilder}.
	 */
	public MappingJackson2CborHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.cbor().build());
	}

	/**
	 * Construct a new {@code MappingJackson2CborHttpMessageConverter} with a
	 * custom {@link ObjectMapper} (must be configured with a {@code CBORFactory}
	 * instance).
	 * <p>You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
	 * @see Jackson2ObjectMapperBuilder#cbor()
	 */
	public MappingJackson2CborHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_CBOR);
		Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
	}


	/**
	 * {@inheritDoc}
	 * <p>The {@code ObjectMapper} must be configured with a {@code CBORFactory} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
		super.setObjectMapper(objectMapper);
	}

}
