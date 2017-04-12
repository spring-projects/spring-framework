/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * <p>Compatible with Jackson 2.6 and higher.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
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
	 * The {@code ObjectMapper} must be configured with a {@code SmileFactory} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(SmileFactory.class, objectMapper.getFactory(), "SmileFactory required");
		super.setObjectMapper(objectMapper);
	}

}
