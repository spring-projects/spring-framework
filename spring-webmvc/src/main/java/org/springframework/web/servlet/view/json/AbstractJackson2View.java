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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractJacksonView;
import org.springframework.web.servlet.view.AbstractView;

/**
 * Abstract base class for Jackson 2.x based and content type independent
 * {@link AbstractView} implementations.
 *
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.1
 * @deprecated since 7.0 in favor of {@link AbstractJacksonView}
 */
@Deprecated(since = "7.0", forRemoval = true)
@SuppressWarnings("removal")
public abstract class AbstractJackson2View extends AbstractView {

	private ObjectMapper objectMapper;

	private JsonEncoding encoding = JsonEncoding.UTF8;

	private @Nullable Boolean prettyPrint;

	private boolean disableCaching = true;

	protected boolean updateContentLength = false;


	protected AbstractJackson2View(ObjectMapper objectMapper, String contentType) {
		this.objectMapper = objectMapper;
		configurePrettyPrint();
		setContentType(contentType);
		setExposePathVariables(false);
	}

	/**
	 * Set the {@code ObjectMapper} for this view.
	 * If not set, a default {@link ObjectMapper#ObjectMapper() ObjectMapper} will be used.
	 * <p>Setting a custom-configured {@code ObjectMapper} is one way to take further control of
	 * the JSON serialization process. The other option is to use Jackson's provided annotations
	 * on the types to be serialized, in which case a custom-configured ObjectMapper is unnecessary.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * Return the {@code ObjectMapper} for this view.
	 */
	public final ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * Set the {@code JsonEncoding} for this view.
	 * By default, {@linkplain JsonEncoding#UTF8 UTF-8} is used.
	 */
	public void setEncoding(JsonEncoding encoding) {
		Assert.notNull(encoding, "'encoding' must not be null");
		this.encoding = encoding;
	}

	/**
	 * Return the {@code JsonEncoding} for this view.
	 */
	public final JsonEncoding getEncoding() {
		return this.encoding;
	}

	/**
	 * Whether to use the default pretty printer when writing the output.
	 * This is a shortcut for setting up an {@code ObjectMapper} as follows:
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * </pre>
	 * <p>The default value is {@code false}.
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}

	/**
	 * Disables caching of the generated JSON.
	 * <p>Default is {@code true}, which will prevent the client from caching the generated JSON.
	 */
	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	/**
	 * Whether to update the 'Content-Length' header of the response. When set to
	 * {@code true}, the response is buffered in order to determine the content
	 * length and set the 'Content-Length' header of the response.
	 * <p>The default setting is {@code false}.
	 */
	public void setUpdateContentLength(boolean updateContentLength) {
		this.updateContentLength = updateContentLength;
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		setResponseContentType(request, response);
		response.setCharacterEncoding(this.encoding.getJavaName());
		if (this.disableCaching) {
			response.addHeader("Cache-Control", "no-store");
		}
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		ByteArrayOutputStream temporaryStream = null;
		OutputStream stream;

		if (this.updateContentLength) {
			temporaryStream = createTemporaryOutputStream();
			stream = temporaryStream;
		}
		else {
			stream = response.getOutputStream();
		}

		Object value = filterAndWrapModel(model, request);
		writeContent(stream, value);

		if (temporaryStream != null) {
			writeToResponse(response, temporaryStream);
		}
	}

	/**
	 * Filter and optionally wrap the model in {@link MappingJacksonValue} container.
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @param request current HTTP request
	 * @return the wrapped or unwrapped value to be rendered
	 */
	protected Object filterAndWrapModel(Map<String, Object> model, HttpServletRequest request) {
		Object value = filterModel(model);
		Class<?> serializationView = (Class<?>) model.get(JsonView.class.getName());
		FilterProvider filters = (FilterProvider) model.get(FilterProvider.class.getName());
		if (serializationView != null || filters != null) {
			MappingJacksonValue container = new MappingJacksonValue(value);
			if (serializationView != null) {
				container.setSerializationView(serializationView);
			}
			if (filters != null) {
				container.setFilters(filters);
			}
			value = container;
		}
		return value;
	}

	/**
	 * Write the actual JSON content to the stream.
	 * @param stream the output stream to use
	 * @param object the value to be rendered, as returned from {@link #filterModel}
	 * @throws IOException if writing failed
	 */
	protected void writeContent(OutputStream stream, Object object) throws IOException {
		try (JsonGenerator generator = this.objectMapper.getFactory().createGenerator(stream, this.encoding)) {
			writePrefix(generator, object);

			Object value = object;
			Class<?> serializationView = null;
			FilterProvider filters = null;

			if (value instanceof MappingJacksonValue container) {
				value = container.getValue();
				serializationView = container.getSerializationView();
				filters = container.getFilters();
			}

			ObjectWriter objectWriter = (serializationView != null ?
					this.objectMapper.writerWithView(serializationView) : this.objectMapper.writer());
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}
			objectWriter.writeValue(generator, value);

			writeSuffix(generator, object);
			generator.flush();
		}
	}


	/**
	 * Set the attribute in the model that should be rendered by this view.
	 * When set, all other model attributes will be ignored.
	 */
	public abstract void setModelKey(String modelKey);

	/**
	 * Filter out undesired attributes from the given model.
	 * The return value can be either another {@link Map} or a single value object.
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @return the value to be rendered
	 */
	protected abstract Object filterModel(Map<String, Object> model);

	/**
	 * Write a prefix before the main content.
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message.
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * Write a suffix after the main content.
	 * @param generator the generator to use for writing content.
	 * @param object the object to write to the output message.
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

}
