/*
 * Copyright 2002-2017 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code View} that writes model attribute(s) with an {@link HttpMessageWriter}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpMessageWriterView implements View {

	private final HttpMessageWriter<?> writer;

	private final Set<String> modelKeys = new HashSet<>(4);

	private final boolean canWriteMap;


	/**
	 * Constructor with an {@code Encoder}.
	 */
	public HttpMessageWriterView(Encoder<?> encoder) {
		this(new EncoderHttpMessageWriter<>(encoder));
	}

	/**
	 * Constructor with a fully initialized {@link HttpMessageWriter}.
	 */
	public HttpMessageWriterView(HttpMessageWriter<?> writer) {
		Assert.notNull(writer, "'writer' is required.");
		this.writer = writer;
		this.canWriteMap = writer.canWrite(ResolvableType.forClass(Map.class), null);
	}


	/**
	 * Return the configured message writer.
	 */
	public HttpMessageWriter<?> getMessageWriter() {
		return this.writer;
	}

	/**
	 * {@inheritDoc}
	 * <p>The implementation of this method for {@link HttpMessageWriterView}
	 * delegates to {@link HttpMessageWriter#getWritableMediaTypes()}.
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.writer.getWritableMediaTypes();
	}

	/**
	 * Set the attributes in the model that should be rendered by this view.
	 * When set, all other model attributes will be ignored. The matching
	 * attributes are further narrowed with {@link HttpMessageWriter#canWrite}.
	 * The matching attributes are processed as follows:
	 * <ul>
	 * <li>0: nothing is written to the response body.
	 * <li>1: the matching attribute is passed to the writer.
	 * <li>2..N: if the writer supports {@link Map}, write all matches;
	 * otherwise raise an {@link IllegalStateException}.
	 * </ul>
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
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
	@SuppressWarnings("unchecked")
	public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
			ServerWebExchange exchange) {

		Object value = getObjectToRender(model);
		return (value != null) ?
				write(value, contentType, exchange) :
				exchange.getResponse().setComplete();
	}

	@Nullable
	private Object getObjectToRender(@Nullable Map<String, ?> model) {
		if (model == null) {
			return null;
		}

		Map<String, ?> result = model.entrySet().stream()
				.filter(this::isMatch)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		if (result.isEmpty()) {
			return null;
		}
		else if (result.size() == 1) {
			return result.values().iterator().next();
		}
		else if (this.canWriteMap) {
			return result;
		}
		else {
			throw new IllegalStateException("Multiple matches found: " + result + " but " +
					"Map rendering is not supported by " + getMessageWriter().getClass().getName());
		}
	}

	private boolean isMatch(Map.Entry<String, ?> entry) {
		if (entry.getValue() == null) {
			return false;
		}
		if (!getModelKeys().isEmpty() && !getModelKeys().contains(entry.getKey())) {
			return false;
		}
		ResolvableType type = ResolvableType.forInstance(entry.getValue());
		return getMessageWriter().canWrite(type, null);
	}

	@SuppressWarnings("unchecked")
	private <T> Mono<Void> write(T value, @Nullable MediaType contentType, ServerWebExchange exchange) {
		Publisher<T> input = Mono.justOrEmpty(value);
		ResolvableType elementType = ResolvableType.forClass(value.getClass());
		return ((HttpMessageWriter<T>) this.writer).write(
				input, elementType, contentType, exchange.getResponse(), Collections.emptyMap());
	}

}
