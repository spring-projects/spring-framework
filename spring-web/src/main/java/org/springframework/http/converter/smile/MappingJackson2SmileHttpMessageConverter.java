/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.smile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write Smile data format ("binary JSON") using
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/master/smile">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports {@code "application/x-jackson-smile"} media type.
 * This can be overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @deprecated since 7.0 in favor of {@link JacksonSmileHttpMessageConverter}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class MappingJackson2SmileHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * Construct a new {@code MappingJackson2SmileHttpMessageConverter} using default configuration
	 * provided by {@code Jackson2ObjectMapperBuilder}.
	 */
	public MappingJackson2SmileHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.smile().build());
	}

	/**
	 * Construct a new {@code MappingJackson2SmileHttpMessageConverter} with a custom {@link ObjectMapper}
	 * (must be configured with a {@code SmileFactory} instance).
	 * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
	 * @see Jackson2ObjectMapperBuilder#smile()
	 */
	public MappingJackson2SmileHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, new MediaType("application", "x-jackson-smile"));
		Assert.isInstanceOf(SmileFactory.class, objectMapper.getFactory(), "SmileFactory required");
	}


	/**
	 * {@inheritDoc}
	 * <p>The {@code ObjectMapper} must be configured with a {@code SmileFactory} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(SmileFactory.class, objectMapper.getFactory(), "SmileFactory required");
		super.setObjectMapper(objectMapper);
	}

}
