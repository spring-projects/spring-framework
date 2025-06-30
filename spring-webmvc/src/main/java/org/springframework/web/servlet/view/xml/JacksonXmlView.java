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

package org.springframework.web.servlet.view.xml;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractJacksonView;

/**
 * Spring MVC {@link View} that renders XML content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 3's</a> {@link XmlMapper}.
 *
 * <p>The Object to be serialized is supplied as a parameter in the model. The first serializable
 * entry is used. Users can specify a specific entry in the model via the
 * {@link #setModelKey(String) sourceKey} property.
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
 * @see org.springframework.web.servlet.view.json.JacksonJsonView
 */
public class JacksonXmlView extends AbstractJacksonView {

	/**
	 * Default content type: {@value}.
	 * <p>Overridable through {@link #setContentType(String)}.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	private @Nullable String modelKey;


	/**
	 * Construct a new instance with an {@link XmlMapper} customized with
	 * the {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)} and setting
	 * the content type to {@code application/xml}.
	 */
	public JacksonXmlView() {
		super(XmlMapper.builder(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Construct a new instance using the provided {@link XmlMapper}
	 * and setting the content type to {@code application/xml}.
	 */
	public JacksonXmlView(XmlMapper xmlMapper) {
		super(xmlMapper, DEFAULT_CONTENT_TYPE);
	}


	@Override
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected Object filterModel(Map<String, Object> model, HttpServletRequest request) {
		Object value = null;
		if (this.modelKey != null) {
			value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException(
						"Model contains no object with key [" + this.modelKey + "]");
			}
		}
		else {
			for (Map.Entry<String, Object> entry : model.entrySet()) {
				if (!(entry.getValue() instanceof BindingResult) &&
						!entry.getKey().equals(JSON_VIEW_HINT) &&
						!entry.getKey().equals(FILTER_PROVIDER_HINT)) {
					if (value != null) {
						throw new IllegalStateException("Model contains more than one object to render, only one is supported");
					}
					value = entry.getValue();
				}
			}
		}
		Assert.state(value != null, "Model contains no object to render");
		return value;
	}

}
