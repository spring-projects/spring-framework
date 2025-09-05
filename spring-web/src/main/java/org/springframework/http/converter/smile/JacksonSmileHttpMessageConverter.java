/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.converter.smile;

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * that can read and write Smile data format ("binary JSON") using
 * <a href="https://github.com/FasterXML/jackson-dataformats-binary/tree/3.x/smile">
 * the dedicated Jackson 3.x extension</a>.
 *
 * <p>By default, this converter supports {@code "application/x-jackson-smile"}
 * media type. This can be overridden by setting the
 * {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * <p>The default constructor loads {@link tools.jackson.databind.JacksonModule}s
 * found by {@link MapperBuilder#findModules(ClassLoader)}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonSmileHttpMessageConverter extends AbstractJacksonHttpMessageConverter<SmileMapper> {

	private static final MediaType DEFAULT_SMILE_MIME_TYPES = new MediaType("application", "x-jackson-smile");

	/**
	 * Construct a new instance with a {@link SmileMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonSmileHttpMessageConverter() {
		super(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper}.
	 * @see SmileMapper#builder()
	 * @see MapperBuilder#findAndAddModules(ClassLoader)
	 */
	public JacksonSmileHttpMessageConverter(SmileMapper mapper) {
		super(mapper, DEFAULT_SMILE_MIME_TYPES);
	}

}
