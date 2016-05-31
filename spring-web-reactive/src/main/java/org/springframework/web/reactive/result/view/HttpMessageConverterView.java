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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;


/**
 * A {@link View} that delegates to an {@link HttpMessageConverter}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpMessageConverterView implements View {

	private final HttpMessageConverter<?> converter;

	private final Set<String> modelKeys = new HashSet<>(4);

	private final List<MediaType> mediaTypes;


	public HttpMessageConverterView(HttpMessageConverter<?> converter) {
		Assert.notNull(converter, "'converter' is required.");
		this.converter = converter;
		this.mediaTypes = converter.getWritableMediaTypes();
	}


	public HttpMessageConverter<?> getConverter() {
		return this.converter;
	}

	/**
	 * By default model attributes are filtered with
	 * {@link HttpMessageConverter#canWrite} to find the ones that can be
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
	public Mono<Void> render(HandlerResult result, MediaType contentType, ServerWebExchange exchange) {
		Object value = extractObjectToRender(result);
		return applyConverter(value, contentType, exchange);
	}

	protected Object extractObjectToRender(HandlerResult result) {
		ModelMap model = result.getModel();
		Map<String, Object> map = new HashMap<>(model.size());
		for (Map.Entry<String, Object> entry : model.entrySet()) {
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
		else if (getConverter().canWrite(ResolvableType.forClass(Map.class), null)) {
			return map;
		}
		else {
			throw new IllegalStateException(
					"Multiple matching attributes found: " + map + ". " +
							"However Map rendering is not supported by " + getConverter());
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
			return getConverter().canWrite(type, null);
		}
		if (getModelKeys().contains(attributeName)) {
			if (getConverter().canWrite(type, null)) {
				return true;
			}
			throw new IllegalStateException(
					"Model object [" + attributeValue + "] retrieved via key " +
							"[" + attributeName + "] is not supported by " + getConverter());
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> Mono<Void> applyConverter(Object value, MediaType contentType, ServerWebExchange exchange) {
		if (value == null) {
			return Mono.empty();
		}
		Publisher<? extends T> stream = Mono.just((T) value);
		ResolvableType type = ResolvableType.forClass(value.getClass());
		ServerHttpResponse response = exchange.getResponse();
		return ((HttpMessageConverter<T>) getConverter()).write(stream, type, contentType, response);
	}

}
