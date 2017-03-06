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

package org.springframework.web.reactive;

import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareConcurrentModel;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.server.ServerWebExchange;

/**
 * Context to assist with processing a request and binding it onto Objects.
 *
 * <p>Provides  methods to create a {@link WebExchangeDataBinder} for a specific
 * target, command Object to apply data binding and validation to, or without a
 * target Object for simple type conversion from request values.
 *
 * <p>Container for the default model for the request.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class BindingContext {

	private final WebBindingInitializer initializer;

	private final Model model = new BindingAwareConcurrentModel();


	public BindingContext() {
		this(null);
	}

	public BindingContext(WebBindingInitializer initializer) {
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
	 *
	 * @param exchange the current exchange
	 * @param target the object to create a data binder for
	 * @param name the name of the target object
	 *
	 * @return the created data binder
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, Object target, String name) {
		WebExchangeDataBinder dataBinder = new WebExchangeDataBinder(target, name);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initDataBinder(dataBinder, exchange);
	}

	/**
	 * Initialize the data binder instance for the given exchange.
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder;
	}

	/**
	 * Create a {@link WebExchangeDataBinder} without a target object for type
	 * conversion of request values to simple types.
	 *
	 * @param exchange the current exchange
	 * @param name the name of the target object
	 *
	 * @return the created data binder
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name);
	}

}
