/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import reactor.core.publisher.Mono;

import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.support.BindingAwareConcurrentModel;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Context to assist with binding request data onto Objects and provide access
 * to a shared {@link Model} with controller-specific attributes.
 *
 * <p>Provides  methods to create a {@link WebExchangeDataBinder} for a specific
 * target, command Object to apply data binding and validation to, or without a
 * target Object for simple type conversion from request values.
 *
 * <p>Container for the default model for the request.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class BindingContext {

	@Nullable
	private final WebBindingInitializer initializer;

	private final Model model = new BindingAwareConcurrentModel();


	/**
	 * Create a new {@code BindingContext}.
	 */
	public BindingContext() {
		this(null);
	}

	/**
	 * Create a new {@code BindingContext} with the given initializer.
	 * @param initializer the binding initializer to apply (may be {@code null})
	 */
	public BindingContext(@Nullable WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * Return the default model.
	 */
	public Model getModel() {
		return this.model;
	}


	/**
	 * Create a {@link WebExchangeDataBinder} to apply data binding and
	 * validation with on the target, command object.
	 * @param exchange the current exchange
	 * @param target the object to create a data binder for
	 * @param name the name of the target object
	 * @return the created data binder
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, @Nullable Object target, String name) {
		WebExchangeDataBinder dataBinder = new ExtendedWebExchangeDataBinder(target, name);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initDataBinder(dataBinder, exchange);
	}

	/**
	 * Initialize the data binder instance for the given exchange.
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder;
	}

	/**
	 * Create a {@link WebExchangeDataBinder} without a target object for type
	 * conversion of request values to simple types.
	 * @param exchange the current exchange
	 * @param name the name of the target object
	 * @return the created data binder
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name);
	}


	/**
	 * Extended variant of {@link WebExchangeDataBinder}, adding path variables.
	 */
	private static class ExtendedWebExchangeDataBinder extends WebExchangeDataBinder {

		public ExtendedWebExchangeDataBinder(@Nullable Object target, String objectName) {
			super(target, objectName);
		}

		@Override
		public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
			Map<String, String> vars = exchange.getAttributeOrDefault(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
			MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
			Mono<MultiValueMap<String, String>> formData = exchange.getFormData();
			Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();

			return Mono.zip(Mono.just(vars), Mono.just(queryParams), formData, multipartData)
					.map(tuple -> {
						Map<String, Object> result = new TreeMap<>();
						tuple.getT1().forEach(result::put);
						tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
						tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));
						tuple.getT4().forEach((key, values) -> addBindValue(result, key, values));
						return result;
					});
		}
	}

}
