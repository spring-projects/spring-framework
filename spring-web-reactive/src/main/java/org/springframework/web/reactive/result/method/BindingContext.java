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

import org.springframework.beans.TypeConverter;
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
public class BindingContext {

	private final ModelMap model = new BindingAwareModelMap();

	private final WebBindingInitializer initializer;

	private final TypeConverter simpleValueTypeConverter;


	public BindingContext() {
		this(null);
	}

	public BindingContext(WebBindingInitializer initializer) {
		this.initializer = initializer;
		this.simpleValueTypeConverter = initTypeConverter(initializer);
	}

	private static WebExchangeDataBinder initTypeConverter(WebBindingInitializer initializer) {
		WebExchangeDataBinder binder = new WebExchangeDataBinder(null);
		if (initializer != null) {
			initializer.initBinder(binder);
		}
		return binder;
	}


	/**
	 * Return the default model.
	 */
	public ModelMap getModel() {
		return this.model;
	}


	/**
	 * Create a {@link WebExchangeDataBinder} for applying data binding, type
	 * conversion, and validation on the given "target" object.
	 *
	 * @param exchange the current exchange
	 * @param target the object to create a data binder for
	 * @param name the name of the target object
	 * @return the {@link WebDataBinder} instance
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, Object target, String name) {
		WebExchangeDataBinder dataBinder = createBinderInstance(target, name);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initDataBinder(dataBinder, exchange);
	}

	/**
	 * Create a {@link WebExchangeDataBinder} without a "target" object, i.e.
	 * for applying type conversion to simple types.
	 *
	 * @param exchange the current exchange
	 * @param name the name of the target object
	 * @return a Mono for the created {@link WebDataBinder} instance
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name);
	}

	/**
	 * Create the data binder instance.
	 */
	protected WebExchangeDataBinder createBinderInstance(Object target, String objectName) {
		return new WebExchangeDataBinder(target, objectName);
	}

	/**
	 * Initialize the data binder instance for the given exchange.
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder,
			ServerWebExchange exchange) {

		return binder;
	}

}
