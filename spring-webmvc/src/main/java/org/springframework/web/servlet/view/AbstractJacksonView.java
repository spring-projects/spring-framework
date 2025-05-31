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

package org.springframework.web.servlet.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.ser.FilterProvider;

import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract base class for Jackson 3.x based and content type independent
 * {@link AbstractView} implementations.
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
public abstract class AbstractJacksonView extends AbstractView {

	protected static final String JSON_VIEW_HINT = JsonView.class.getName();

	protected static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();


	private static volatile @Nullable List<JacksonModule> modules;


	private final ObjectMapper objectMapper;

	private JsonEncoding encoding = JsonEncoding.UTF8;

	private boolean disableCaching = true;

	protected boolean updateContentLength = false;


	protected AbstractJacksonView(MapperBuilder<?, ?> builder, String contentType) {
		this.objectMapper = builder.addModules(initModules()).build();
		setContentType(contentType);
		setExposePathVariables(false);
	}

	protected AbstractJacksonView(ObjectMapper objectMapper, String contentType) {
		this.objectMapper = objectMapper;
		setContentType(contentType);
		setExposePathVariables(false);
	}


	private List<JacksonModule> initModules() {
		if (modules == null) {
			modules = MapperBuilder.findModules(AbstractJacksonHttpMessageConverter.class.getClassLoader());

		}
		return Objects.requireNonNull(modules);
	}

	/**
	 * Set the {@code JsonEncoding} for this view.
	 * <p>Default is {@linkplain JsonEncoding#UTF8 UTF-8}.
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
	 * Disables caching of the generated JSON.
	 * <p>Default is {@code true}, which will prevent the client from caching the
	 * generated JSON.
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

		Object value = filterModel(model, request);
		Map<String, Object> hints = null;
		boolean containsFilterProviderHint = model.containsKey(FILTER_PROVIDER_HINT);
		if (model.containsKey(JSON_VIEW_HINT)) {
			if (containsFilterProviderHint) {
				hints = new HashMap<>(2);
				hints.put(JSON_VIEW_HINT, model.get(JSON_VIEW_HINT));
				hints.put(FILTER_PROVIDER_HINT, model.get(FILTER_PROVIDER_HINT));
			}
			else {
				hints = Collections.singletonMap(JSON_VIEW_HINT, model.get(JSON_VIEW_HINT));
			}
		}
		else if (containsFilterProviderHint) {
			hints = Collections.singletonMap(FILTER_PROVIDER_HINT, model.get(FILTER_PROVIDER_HINT));
		}
		writeContent(stream, value, hints);

		if (temporaryStream != null) {
			writeToResponse(response, temporaryStream);
		}
	}

	/**
	 * Write the actual JSON content to the stream.
	 * @param stream the output stream to use
	 * @param object the value to be rendered, as returned from {@link #filterModel}
	 * @param hints additional information about how to serialize the data
	 * @throws IOException if writing failed
	 */
	protected void writeContent(OutputStream stream, Object object, @Nullable Map<String, Object> hints) throws IOException {
		try (JsonGenerator generator = this.objectMapper.createGenerator(stream, this.encoding)) {
			writePrefix(generator, object);

			Class<?> serializationView = null;
			FilterProvider filters = null;
			if (hints != null) {
				serializationView = (Class<?>) hints.get(JSON_VIEW_HINT);
				filters = (FilterProvider) hints.get(FILTER_PROVIDER_HINT);
			}

			ObjectWriter objectWriter = (serializationView != null ?
					this.objectMapper.writerWithView(serializationView) : this.objectMapper.writer());
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}
			objectWriter.writeValue(generator, object);

			writeSuffix(generator, object);
			generator.flush();
		}
	}


	/**
	 * Set the attribute in the model that should be rendered by this view.
	 * <p>When set, all other model attributes will be ignored.
	 */
	public abstract void setModelKey(String modelKey);

	/**
	 * Filter out undesired attributes from the given model.
	 * <p>The return value can be either another {@link Map} or a single value object.
	 * @param model the model, as passed on to {@link #renderMergedOutputModel}
	 * @param request current HTTP request
	 * @return the value to be rendered
	 */
	protected abstract Object filterModel(Map<String, Object> model, HttpServletRequest request);

	/**
	 * Write a prefix before the main content.
	 * @param generator the generator to use for writing content
	 * @param object the object to write to the output message
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * Write a suffix after the main content.
	 * @param generator the generator to use for writing content
	 * @param object the object to write to the output message
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

}
