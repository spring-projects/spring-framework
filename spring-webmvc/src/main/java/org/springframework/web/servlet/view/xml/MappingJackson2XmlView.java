/*
 * Copyright 2002-2025 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jspecify.annotations.Nullable;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.json.AbstractJackson2View;

/**
 * Spring MVC {@link View} that renders XML content by serializing the model for the current request
 * using <a href="https://github.com/FasterXML/jackson">Jackson 2's</a> {@link XmlMapper}.
 *
 * <p>The Object to be serialized is supplied as a parameter in the model. The first serializable
 * entry is used. Users can either specify a specific entry in the model via the
 * {@link #setModelKey(String) sourceKey} property.
 *
 * <p>The default constructor uses the default configuration provided by {@link Jackson2ObjectMapperBuilder}.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 * @see org.springframework.web.servlet.view.json.MappingJackson2JsonView
 * @deprecated since 7.0 in favor of {@link JacksonXmlView}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public class MappingJackson2XmlView extends AbstractJackson2View {

	/**
	 * The default content type for the view.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	private @Nullable String modelKey;


	/**
	 * Construct a new {@code MappingJackson2XmlView} using default configuration
	 * provided by {@link Jackson2ObjectMapperBuilder} and setting the content type
	 * to {@code application/xml}.
	 */
	public MappingJackson2XmlView() {
		super(Jackson2ObjectMapperBuilder.xml().build(), DEFAULT_CONTENT_TYPE);
	}

	/**
	 * Construct a new {@code MappingJackson2XmlView} using the provided {@link XmlMapper}
	 * and setting the content type to {@code application/xml}.
	 * @since 4.2.1
	 */
	public MappingJackson2XmlView(XmlMapper xmlMapper) {
		super(xmlMapper, DEFAULT_CONTENT_TYPE);
	}


	@Override
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected Object filterModel(Map<String, Object> model) {
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
				if (!(entry.getValue() instanceof BindingResult) && !entry.getKey().equals(JsonView.class.getName())) {
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
