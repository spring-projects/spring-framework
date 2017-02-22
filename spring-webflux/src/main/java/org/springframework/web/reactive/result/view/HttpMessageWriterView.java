/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;


/**
 * A {@link View} that delegates to an {@link HttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpMessageWriterView implements View {

	private final HttpMessageWriter<?> messageWriter;

	private final Set<String> modelKeys = new HashSet<>(4);

	private final List<MediaType> mediaTypes;


	/**
	 * Create a {@code View} with the given {@code Encoder} wrapping it as an
	 * {@link EncoderHttpMessageWriter}.
	 */
	public HttpMessageWriterView(Encoder<?> encoder) {
		this(new EncoderHttpMessageWriter<>(encoder));
	}

	/**
	 * Create a View that delegates to the given message messageWriter.
	 */
	public HttpMessageWriterView(HttpMessageWriter<?> messageWriter) {
		Assert.notNull(messageWriter, "'messageWriter' is required.");
		this.messageWriter = messageWriter;
		this.mediaTypes = messageWriter.getWritableMediaTypes();
	}


	/**
	 * Return the configured message messageWriter.
	 */
	public HttpMessageWriter<?> getMessageWriter() {
		return this.messageWriter;
	}

	/**
	 * By default model attributes are filtered with
	 * {@link HttpMessageWriter#canWrite} to find the ones that can be
	 * rendered. Use this property to further narrow the list and consider only
	 * attribute(s) under specific model key(s).
	 * <p>If more than one matching attribute is found, than a Map is rendered,
	 * or if the {@code Encoder} does not support rendering a {@code Map} then
	 * an exception is raised.
	 */
	public void setModelKeys(Set<String> modelKeys) {
		this.modelKeys.clear();
		if (modelKeys != null) {
			this.modelKeys.addAll(modelKeys);
		}
	}

	/**
	 * Return the configured model keys.
	 */
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	@Override
	public Mono<Void> render(Map<String, ?> model, MediaType contentType,
			ServerWebExchange exchange) {
		Object value = extractObjectToRender(model);
		return applyMessageWriter(value, contentType, exchange);
	}

	protected Object extractObjectToRender(Map<String, ?> model) {
		Map<String, Object> map = new HashMap<>(model.size());
		for (Map.Entry<String, ?> entry : model.entrySet()) {
			if (isEligibleAttribute(entry.getKey(), entry.getValue())) {
				map.put(entry.getKey(), entry.getValue());
			}
		}
		if (map.isEmpty()) {
			return null;
		}
		else if (map.size() == 1) {
			return map.values().iterator().next();
		}
		else if (getMessageWriter().canWrite(ResolvableType.forClass(Map.class), null)) {
			return map;
		}
		else {
			throw new IllegalStateException(
					"Multiple matching attributes found: " + map + ". " +
							"However Map rendering is not supported by " + getMessageWriter());
		}
	}

	/**
	 * Whether the given model attribute key-value pair is eligible for encoding.
	 * <p>The default implementation checks against the configured
	 * {@link #setModelKeys model keys} and whether the Encoder supports the
	 * value type.
	 */
	protected boolean isEligibleAttribute(String attributeName, Object attributeValue) {
		ResolvableType type = ResolvableType.forClass(attributeValue.getClass());
		if (getModelKeys().isEmpty()) {
			return getMessageWriter().canWrite(type, null);
		}
		if (getModelKeys().contains(attributeName)) {
			if (getMessageWriter().canWrite(type, null)) {
				return true;
			}
			throw new IllegalStateException(
					"Model object [" + attributeValue + "] retrieved via key " +
							"[" + attributeName + "] is not supported by " + getMessageWriter());
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> Mono<Void> applyMessageWriter(Object value, MediaType contentType, ServerWebExchange exchange) {
		if (value == null) {
			return Mono.empty();
		}
		Publisher<? extends T> stream = Mono.just((T) value);
		ResolvableType type = ResolvableType.forClass(value.getClass());
		ServerHttpResponse response = exchange.getResponse();
		return ((HttpMessageWriter<T>) getMessageWriter()).write(stream, type, contentType,
				response, Collections.emptyMap());
	}

}
