/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.view.json;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;

/**
 * Spring MVC {@link View} that renders JSON content by serializing the model for the current request
 * using <a href="http://wiki.fasterxml.com/JacksonHome">Jackson 2's</a> {@link ObjectMapper}.
 *
 * <p>By default, the entire contents of the model map (with the exception of framework-specific classes)
 * will be encoded as JSON. If the model contains only one key, you can have it extracted encoded as JSON
 * alone via  {@link #setExtractValueFromSingleKeyModel}.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * <p>Compatible with Jackson 2.6 and higher, as of Spring 4.3.
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.1.2
 */
public class MappingJackson2JsonView extends AbstractJackson2View {

	/**
	 * Default content type: "application/json".
	 * Overridable through {@link #setContentType}.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";

	@Nullable
	private String jsonPrefix;

	@Nullable
	private Set<String> modelKeys;

	private boolean extractValueFromSingleKeyModel = false;


	/**
	 * Construct a new {@code MappingJackson2JsonView} using default configuration
	 * provided by {@link Jackson2ObjectMapperBuilder} and setting the content type
	 * to {@code application/json}.
	 */
	public MappingJackson2JsonView() {
		super(Jackson2ObjectMapperBuilder.json().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Construct a new {@code MappingJackson2JsonView} using the provided
	 * {@link ObjectMapper} and setting the content type to {@code application/json}.
	 * @since 4.2.1
	 */
	public MappingJackson2JsonView(ObjectMapper objectMapper) {
		super(objectMapper, DEFAULT_CONTENT_TYPE);
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
	 * Indicates whether the JSON output by this view should be prefixed with <tt>")]}', "</tt>.
	 * Default is {@code false}.
	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
	 * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
	 * This prefix should be stripped before parsing the string as JSON.
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setModelKey(String modelKey) {
		this.modelKeys = Collections.singleton(modelKey);
	}

	/**
	 * Set the attributes in the model that should be rendered by this view.
	 * When set, all other model attributes will be ignored.
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys = modelKeys;
	}

	/**
	 * Return the attributes in the model that should be rendered by this view.
	 */
	@Nullable
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * Set whether to serialize models containing a single attribute as a map or
	 * whether to extract the single value from the model and serialize it directly.
	 * <p>The effect of setting this flag is similar to using
	 * {@code MappingJackson2HttpMessageConverter} with an {@code @ResponseBody}
	 * request-handling method.
	 * <p>Default is {@code false}.
	 */
	public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
		this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
	}

	/**
	 * Filter out undesired attributes from the given model.
	 * The return value can be either another {@link Map} or a single value object.
	 * <p>The default implementation removes {@link BindingResult} instances and entries
	 * not included in the {@link #setModelKeys renderedAttributes} property.
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @return the value to be rendered
	 */
	@Override
	protected Object filterModel(Map<String, Object> model) {
		Map<String, Object> result = new HashMap<>(model.size());
		Set<String> modelKeys = (!CollectionUtils.isEmpty(this.modelKeys) ? this.modelKeys : model.keySet());
		model.forEach((clazz, value) -> {
			if (!(value instanceof BindingResult) && modelKeys.contains(clazz) &&
					!clazz.equals(JsonView.class.getName()) &&
					!clazz.equals(FilterProvider.class.getName())) {
				result.put(clazz, value);
			}
		});
		return (this.extractValueFromSingleKeyModel && result.size() == 1 ? result.values().iterator().next() : result);
	}

	@Override
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
		if (this.jsonPrefix != null) {
			generator.writeRaw(this.jsonPrefix);
		}
	}

}
