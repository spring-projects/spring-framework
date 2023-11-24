/*
 * Copyright 2002-2022 the original author or authors.
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
 */
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
	 * The {@code ObjectMapper} must be configured with a {@code CBORFactory} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(CBORFactory.class, objectMapper.getFactory(), "CBORFactory required");
		super.setObjectMapper(objectMapper);
	}

}
