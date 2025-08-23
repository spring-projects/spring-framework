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

package org.springframework.web.servlet.view.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractJacksonView;

/**
 * Spring MVC {@link View} that renders JSON content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 3's</a> {@link JsonMapper}.
 *
 * <p>By default, the entire contents of the model map (with the exception of framework-specific classes)
 * will be encoded as JSON. If the model contains only one key, you can have it extracted encoded as JSON
 * alone via {@link #setExtractValueFromSingleKeyModel}.
 *
 * <p>The following special model entries are supported:
 * <ul>
 *     <li>A JSON view with a <code>com.fasterxml.jackson.annotation.JsonView</code>
 *         key and the class name of the JSON view as value.</li>
 *     <li>A filter provider with a <code>tools.jackson.databind.ser.FilterProvider</code>
 *         key and the filter provider class name as value.</li>
 * </ul>
 *
 * @author Sebastien Deleuze
 * @since 7.0
 */
public class JacksonJsonView extends AbstractJacksonView {

	/**
	 * Default content type: {@value}.
	 * <p>Overridable through {@link #setContentType(String)}.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/json";


	private @Nullable String jsonPrefix;

	private @Nullable Set<String> modelKeys;

	private boolean extractValueFromSingleKeyModel = false;


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with
	 * the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)} and setting
	 * the content type to {@code application/json}.
	 */
	public JacksonJsonView() {
		super(JsonMapper.builder(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Construct a new instance using the provided {@link JsonMapper}
	 * and setting the content type to {@value #DEFAULT_CONTENT_TYPE}.
	 */
	public JacksonJsonView(JsonMapper jsonMapper) {
		super(jsonMapper, DEFAULT_CONTENT_TYPE);
	}


	/**
	 * Specify a custom prefix to use for this view's JSON output.
	 * <p>Default is none.
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * Indicates whether the JSON output by this view should be prefixed with <code>")]}', "</code>.
	 * <p>Default is {@code false}.
	 * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
	 * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
	 * This prefix should be stripped before parsing the string as JSON.
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}

	@Override
	public void setModelKey(String modelKey) {
		this.modelKeys = Collections.singleton(modelKey);
	}

	/**
	 * Set the attributes in the model that should be rendered by this view.
	 * <p>When set, all other model attributes will be ignored.
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys = modelKeys;
	}

	/**
	 * Return the attributes in the model that should be rendered by this view.
	 */
	public final @Nullable Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * Set whether to serialize models containing a single attribute as a map or
	 * whether to extract the single value from the model and serialize it directly.
	 * <p>The effect of setting this flag is similar to using
	 * {@code JacksonJsonHttpMessageConverter} with an {@code @ResponseBody}
	 * request-handling method.
	 * <p>Default is {@code false}.
	 */
	public void setExtractValueFromSingleKeyModel(boolean extractValueFromSingleKeyModel) {
		this.extractValueFromSingleKeyModel = extractValueFromSingleKeyModel;
	}

	/**
	 * Filter out undesired attributes from the given model.
	 * <p>The return value can be either another {@link Map} or a single value object.
	 * <p>The default implementation removes {@link BindingResult} instances and entries
	 * not included in the {@link #setModelKeys modelKeys} property.
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @return the value to be rendered
	 */
	@Override
	protected Object filterModel(Map<String, Object> model, HttpServletRequest request) {
		Map<String, Object> result = CollectionUtils.newHashMap(model.size());
		Set<String> modelKeys = (!CollectionUtils.isEmpty(this.modelKeys) ? this.modelKeys : model.keySet());
		model.forEach((clazz, value) -> {
			if (!(value instanceof BindingResult) && modelKeys.contains(clazz) &&
					!clazz.equals(JSON_VIEW_HINT) &&
					!clazz.equals(FILTER_PROVIDER_HINT)) {
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
