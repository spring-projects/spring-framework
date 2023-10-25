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

package org.springframework.web.bind.support;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import reactor.core.publisher.Mono;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.server.ServerWebExchange;

/**
 * Specialized {@link org.springframework.validation.DataBinder} to perform data
 * binding from URL query parameters or form data in the request data to Java objects.
 *
 * <p><strong>WARNING</strong>: Data binding can lead to security issues by exposing
 * parts of the object graph that are not meant to be accessed or modified by
 * external clients. Therefore the design and use of data binding should be considered
 * carefully with regard to security. For more details, please refer to the dedicated
 * sections on data binding for
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> and
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * in the reference manual.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class WebExchangeDataBinder extends WebDataBinder {

	/**
	 * Create a new instance, with default object name.
	 * @param target the target object to bind onto (or {@code null} if the
	 * binder is just used to convert a plain parameter value)
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebExchangeDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * Create a new instance.
	 * @param target the target object to bind onto (or {@code null} if the
	 * binder is just used to convert a plain parameter value)
	 * @param objectName the name of the target object
	 */
	public WebExchangeDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * Use a default or single data constructor to create the target by
	 * binding request parameters, multipart files, or parts to constructor args.
	 * <p>After the call, use {@link #getBindingResult()} to check for bind errors.
	 * If there are none, the target is set, and {@link #bind} can be called for
	 * further initialization via setters.
	 * @param exchange the request to bind
	 * @return a {@code Mono<Void>} that completes when the target is created
	 * @since 6.1
	 */
	public Mono<Void> construct(ServerWebExchange exchange) {
		return getValuesToBind(exchange)
				.doOnNext(map -> construct(new MapValueResolver(map)))
				.then();
	}

	/**
	 * Bind query parameters, form data, or multipart form data to the binder target.
	 * @param exchange the current exchange
	 * @return a {@code Mono<Void>} that completes when binding is complete
	 */
	public Mono<Void> bind(ServerWebExchange exchange) {
		if (shouldNotBindPropertyValues()) {
			return Mono.empty();
		}
		return getValuesToBind(exchange)
				.doOnNext(map -> doBind(new MutablePropertyValues(map)))
				.then();
	}

	/**
	 * Obtain the values for data binding. By default, this delegates to
	 * {@link #extractValuesToBind(ServerWebExchange)}.
	 * @param exchange the current exchange
	 * @return a map of bind values
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
		return extractValuesToBind(exchange);
	}


	/**
	 * Combine query params and form data for multipart form data from the body
	 * of the request into a {@code Map<String, Object>} of values to use for
	 * data binding purposes.
	 * @param exchange the current exchange
	 * @return a {@code Mono} with the values to bind
	 * @see org.springframework.http.server.reactive.ServerHttpRequest#getQueryParams()
	 * @see ServerWebExchange#getFormData()
	 * @see ServerWebExchange#getMultipartData()
	 */
	public static Mono<Map<String, Object>> extractValuesToBind(ServerWebExchange exchange) {
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		Mono<MultiValueMap<String, String>> formData = exchange.getFormData();
		Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();

		return Mono.zip(Mono.just(queryParams), formData, multipartData)
				.map(tuple -> {
					Map<String, Object> result = new TreeMap<>();
					tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
					tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
					tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));
					return result;
				});
	}

	protected static void addBindValue(Map<String, Object> params, String key, List<?> values) {
		if (!CollectionUtils.isEmpty(values)) {
			values = values.stream()
					.map(value -> value instanceof FormFieldPart formFieldPart ? formFieldPart.value() : value)
					.toList();
			params.put(key, values.size() == 1 ? values.get(0) : values);
		}
	}


	/**
	 * Resolve values from a map.
	 */
	private record MapValueResolver(Map<String, Object> map) implements ValueResolver {

		@Override
		public Object resolveValue(String name, Class<?> type) {
			return this.map.get(name);
		}
	}

}
