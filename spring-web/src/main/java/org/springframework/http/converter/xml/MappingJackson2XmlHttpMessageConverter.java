/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.converter.xml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write XML using <a href="https://github.com/FasterXML/jackson-dataformat-xml">
 * Jackson 2.x extension component for reading and writing XML encoded data</a>.
 *
 * <p>By default, this converter supports {@code application/xml}, {@code text/xml}, and
 * {@code application/*+xml} with {@code UTF-8} character set. This can be overridden by
 * setting the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.1 to 2.6.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class MappingJackson2XmlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * Construct a new {@code MappingJackson2XmlHttpMessageConverter} using default configuration
	 * provided by {@code Jackson2ObjectMapperBuilder}.
	 */
	public MappingJackson2XmlHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.xml().build());
	}

	/**
	 * Construct a new {@code MappingJackson2XmlHttpMessageConverter} with a custom {@link ObjectMapper}
	 * (must be a {@link XmlMapper} instance).
	 * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
	 * @see Jackson2ObjectMapperBuilder#xml()
	 */
	public MappingJackson2XmlHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, new MediaType("application", "xml", DEFAULT_CHARSET),
				new MediaType("text", "xml", DEFAULT_CHARSET),
				new MediaType("application", "*+xml", DEFAULT_CHARSET));
		Assert.isAssignable(XmlMapper.class, objectMapper.getClass());
	}

	/**
	 * {@inheritDoc}
	 * The {@code objectMapper} parameter must be a {@link XmlMapper} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isAssignable(XmlMapper.class, objectMapper.getClass());
		super.setObjectMapper(objectMapper);
	}

}
