/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view.xml;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamResult;

import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
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
 * @since 3.0
 */
public class MarshallingView extends AbstractView {

	/**
	 * Default content type. Overridable as bean property.
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";


	private Marshaller marshaller;

	private String modelKey;


	/**
	 * Constructs a new {@code MarshallingView} with no {@link Marshaller} set.
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
		setMarshaller(marshaller);
	}


	/**
	 * Sets the {@link Marshaller} to be used by this view.
	 */
	public void setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
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
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
	}


	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		Object toBeMarshalled = locateToBeMarshalled(model);
		if (toBeMarshalled == null) {
			throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
		this.marshaller.marshal(toBeMarshalled, new StreamResult(bos));

		setResponseContentType(request, response);
		response.setContentLength(bos.size());

		StreamUtils.copy(bos.toByteArray(), response.getOutputStream());
	}

	/**
	 * Locates the object to be marshalled. The default implementation first attempts to look
	 * under the configured {@linkplain #setModelKey(String) model key}, if any, before attempting
	 * to locate an object of {@linkplain Marshaller#supports(Class) supported type}.
	 * @param model the model Map
	 * @return the Object to be marshalled (or {@code null} if none found)
	 * @throws IllegalStateException if the model object specified by the
	 * {@linkplain #setModelKey(String) model key} is not supported by the marshaller
	 * @see #setModelKey(String)
	 */
	protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
		if (this.modelKey != null) {
			Object obj = model.get(this.modelKey);
			if (obj == null) {
				throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
			}
			if (!this.marshaller.supports(obj.getClass())) {
				throw new IllegalStateException("Model object [" + obj + "] retrieved via key [" +
						this.modelKey + "] is not supported by the Marshaller");
			}
			return obj;
		}
		for (Object obj : model.values()) {
			if (obj != null && this.marshaller.supports(obj.getClass())) {
				return obj;
			}
		}
		return null;
	}

}
