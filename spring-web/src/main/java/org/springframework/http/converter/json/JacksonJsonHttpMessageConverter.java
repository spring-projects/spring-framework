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

package org.springframework.http.converter.json;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;

/**
 * Implementation of {@link org.springframework.http.converter.HttpMessageConverter}
 * that can read and write JSON using <a href="https://github.com/FasterXML/jackson">Jackson 3.x's</a>
 * {@link JsonMapper}.
 *
 * <p>This converter can be used to bind to typed beans, or untyped
 * {@code HashMap} instances.
 *
 * <p>By default, this converter supports {@code application/json} and
 * {@code application/*+json} with {@code UTF-8} character set. This
 * can be overridden by setting the {@link #setSupportedMediaTypes supportedMediaTypes}
 * property.
 *
 * <p>The following hints entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>"com.fasterxml.jackson.annotation.JsonView"</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>"tools.jackson.databind.ser.FilterProvider"</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonJsonHttpMessageConverter extends AbstractJacksonHttpMessageConverter<JsonMapper> {

	private static final List<MediaType> problemDetailMediaTypes =
			Collections.singletonList(MediaType.APPLICATION_PROBLEM_JSON);

	private static final MediaType[] DEFAULT_JSON_MIME_TYPES = new MediaType[] {
			MediaType.APPLICATION_JSON, new MediaType("application", "*+json") };


	private @Nullable String jsonPrefix;


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)} and
	 * {@link ProblemDetailJacksonMixin}.
	 */
	public JacksonJsonHttpMessageConverter() {
		this(JsonMapper.builder());
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s found
	 * by {@link MapperBuilder#findModules(ClassLoader)} and
	 * {@link ProblemDetailJacksonMixin}.
	 * @see JsonMapper#builder()
	 */
	public JacksonJsonHttpMessageConverter(JsonMapper.Builder builder) {
		super(builder.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class), DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper}.
	 * @see JsonMapper#builder()
	 */
	public JacksonJsonHttpMessageConverter(JsonMapper mapper) {
		super(mapper, DEFAULT_JSON_MIME_TYPES);
	}


	/**
	 * Specify a custom prefix to use for this view's JSON output.
	 * Default is none.
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * Indicate whether the JSON output by this view should be prefixed with ")]}', ". Default is {@code false}.
	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
	 * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
	 * This prefix should be stripped before parsing the string as JSON.
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	protected List<MediaType> getMediaTypesForProblemDetail() {
		return problemDetailMediaTypes;
	}

	@Override
	protected void writePrefix(JsonGenerator generator, Object object) {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}
	}

}
