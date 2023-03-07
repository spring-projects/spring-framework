/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBElement;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Spring-MVC {@link View} that allows for response context to be rendered as the result
 * of marshalling by a {@link Marshaller}.
 *
 * <p>The Object to be marshalled is supplied as a parameter in the model and then
 * {@linkplain #locateToBeMarshalled(Map) detected} during response rendering. Users can
 * either specify a specific entry in the model via the {@link #setModelKey(String) sourceKey}
 * property or have Spring locate the Source object.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MarshallingView extends AbstractView {

	/**
	 * Default content type. Overridable as bean property.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	@Nullable
	private Marshaller marshaller;

	@Nullable
	private String modelKey;


	/**
	 * Construct a new {@code MarshallingView} with no {@link Marshaller} set.
	 * The marshaller must be set after construction by invoking {@link #setMarshaller}.
	 */
	public MarshallingView() {
		setContentType(DEFAULT_CONTENT_TYPE);
		setExposePathVariables(false);
	}

	/**
	 * Constructs a new {@code MarshallingView} with the given {@link Marshaller} set.
	 */
	public MarshallingView(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
	}


	/**
	 * Set the {@link Marshaller} to be used by this view.
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * Set the name of the model key that represents the object to be marshalled.
	 * If not specified, the model map will be searched for a supported value type.
	 * @see Marshaller#supports(Class)
	 */
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected void initApplicationContext() {
		Assert.state(this.marshaller != null, "Property 'marshaller' is required");
	}


	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Object toBeMarshalled = locateToBeMarshalled(model);
		if (toBeMarshalled == null) {
			throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
		}

		Assert.state(this.marshaller != null, "No Marshaller set");
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		this.marshaller.marshal(toBeMarshalled, new StreamResult(baos));

		setResponseContentType(request, response);
		response.setContentLength(baos.size());
		baos.writeTo(response.getOutputStream());
	}

	/**
	 * Locate the object to be marshalled.
	 * <p>The default implementation first attempts to look under the configured
	 * {@linkplain #setModelKey(String) model key}, if any, before attempting to
	 * locate an object of {@linkplain Marshaller#supports(Class) supported type}.
	 * @param model the model Map
	 * @return the Object to be marshalled (or {@code null} if none found)
	 * @throws IllegalStateException if the model object specified by the
	 * {@linkplain #setModelKey(String) model key} is not supported by the marshaller
	 * @see #setModelKey(String)
	 */
	@Nullable
	protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
		if (this.modelKey != null) {
			Object value = model.get(this.modelKey);
			if (value == null) {
				throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
			}
			if (!isEligibleForMarshalling(this.modelKey, value)) {
				throw new IllegalStateException("Model object [" + value + "] retrieved via key [" +
						this.modelKey + "] is not supported by the Marshaller");
			}
			return value;
		}
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value != null && (model.size() == 1 || !(value instanceof BindingResult)) &&
					isEligibleForMarshalling(entry.getKey(), value)) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Check whether the given value from the current view's model is eligible
	 * for marshalling through the configured {@link Marshaller}.
	 * <p>The default implementation calls {@link Marshaller#supports(Class)},
	 * unwrapping a given {@link JAXBElement} first if applicable.
	 * @param modelKey the value's key in the model (never {@code null})
	 * @param value the value to check (never {@code null})
	 * @return whether the given value is to be considered as eligible
	 * @see Marshaller#supports(Class)
	 */
	protected boolean isEligibleForMarshalling(String modelKey, Object value) {
		Assert.state(this.marshaller != null, "No Marshaller set");
		Class<?> classToCheck = value.getClass();
		if (value instanceof JAXBElement<?> jaxbElement) {
			classToCheck = jaxbElement.getDeclaredType();
		}
		return this.marshaller.supports(classToCheck);
	}

}
