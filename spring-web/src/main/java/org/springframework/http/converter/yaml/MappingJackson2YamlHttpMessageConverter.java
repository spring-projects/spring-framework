/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.converter.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter
 * HttpMessageConverter} that can read and write the <a href="https://yaml.io/">YAML</a>
 * data format using <a href="https://github.com/FasterXML/jackson-dataformats-text/tree/2.17/yaml">
 * the dedicated Jackson 2.x extension</a>.
 *
 * <p>By default, this converter supports the {@link MediaType#APPLICATION_YAML_VALUE}
 * media type. This can be overridden by setting the {@link #setSupportedMediaTypes
 * supportedMediaTypes} property.
 *
 * <p>The default constructor uses the default configuration provided by
 * {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Hyoungjune Kim
 * @since 6.2
 */
public class MappingJackson2YamlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

	/**
	 * Construct a new {@code MappingJackson2YamlHttpMessageConverter} using the
	 * default configuration provided by {@code Jackson2ObjectMapperBuilder}.
	 */
	public MappingJackson2YamlHttpMessageConverter() {
		this(Jackson2ObjectMapperBuilder.yaml().build());
	}

	/**
	 * Construct a new {@code MappingJackson2YamlHttpMessageConverter} with a
	 * custom {@link ObjectMapper} (must be configured with a {@code YAMLFactory}
	 * instance).
	 * <p>You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
	 * @see Jackson2ObjectMapperBuilder#yaml()
	 */
	public MappingJackson2YamlHttpMessageConverter(ObjectMapper objectMapper) {
		super(objectMapper, MediaType.APPLICATION_YAML);
		Assert.isInstanceOf(YAMLFactory.class, objectMapper.getFactory(), "YAMLFactory required");
	}


	/**
	 * {@inheritDoc}
	 * <p>The {@code ObjectMapper} must be configured with a {@code YAMLFactory} instance.
	 */
	@Override
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.isInstanceOf(YAMLFactory.class, objectMapper.getFactory(), "YAMLFactory required");
		super.setObjectMapper(objectMapper);
	}

}
