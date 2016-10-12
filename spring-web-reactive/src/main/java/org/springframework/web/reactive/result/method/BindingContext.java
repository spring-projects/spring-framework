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
package org.springframework.web.reactive.result.method;

import java.lang.reflect.Field;

import reactor.core.publisher.Mono;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.WebExchangeDataBinder;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.server.ServerWebExchange;

/**
 * A context for binding requests to method arguments that provides access to
 * the default model, data binding, validation, and type conversion.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class BindingContext implements TypeConverter {

	private final ModelMap model = new BindingAwareModelMap();

	private final WebBindingInitializer initializer;

	private final WebDataBinder simpleValueDataBinder;


	public BindingContext() {
		this(null);
	}

	public BindingContext(WebBindingInitializer initializer) {
		this.initializer = initializer;
		this.simpleValueDataBinder = new WebExchangeDataBinder(null);
		if (initializer != null) {
			initializer.initBinder(this.simpleValueDataBinder);
		}
	}


	/**
	 * Return the default model.
	 */
	public ModelMap getModel() {
		return this.model;
	}

	/**
	 * Create a {@link WebExchangeDataBinder} for the given object.
	 * @param exchange the current exchange
	 * @param target the object to create a data binder for, or {@code null} if
	 * creating a binder for a simple type
	 * @param objectName the name of the target object
	 * @return a Mono for the created {@link WebDataBinder} instance
	 */
	public Mono<WebExchangeDataBinder> createBinder(ServerWebExchange exchange, Object target,
			String objectName) {

		WebExchangeDataBinder dataBinder = createBinderInstance(target, objectName);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initBinder(dataBinder, exchange);
	}

	protected WebExchangeDataBinder createBinderInstance(Object target, String objectName) {
		return new WebExchangeDataBinder(target, objectName);
	}

	protected Mono<WebExchangeDataBinder> initBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return Mono.just(binder);
	}

	public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
		return this.simpleValueDataBinder.convertIfNecessary(value, requiredType);
	}

	public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam)
			throws TypeMismatchException {

		return this.simpleValueDataBinder.convertIfNecessary(value, requiredType, methodParam);
	}

	public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field)
			throws TypeMismatchException {

		return this.simpleValueDataBinder.convertIfNecessary(value, requiredType, field);
	}

}
