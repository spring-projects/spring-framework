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

package org.springframework.web.reactive;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.support.BindingAwareConcurrentModel;
import org.springframework.web.bind.support.BindParamNameResolver;
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

	private boolean methodValidationApplicable;

	private final ReactiveAdapterRegistry reactiveAdapterRegistry;


	/**
	 * Create an instance without an initializer.
	 */
	public BindingContext() {
		this(null);
	}

	/**
	 * Create an instance with the given initializer, which may be {@code null}.
	 */
	public BindingContext(@Nullable WebBindingInitializer initializer) {
		this(initializer, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * Create an instance with the given initializer and {@code ReactiveAdapterRegistry}.
	 * @since 6.1
	 */
	public BindingContext(@Nullable WebBindingInitializer initializer, ReactiveAdapterRegistry registry) {
		this.initializer = initializer;
		this.reactiveAdapterRegistry = new ReactiveAdapterRegistry();
	}


	/**
	 * Return the default model.
	 */
	public Model getModel() {
		return this.model;
	}

	/**
	 * Configure flag to signal whether validation will be applied to handler
	 * method arguments, which is the case if Bean Validation is enabled in
	 * Spring MVC, and method parameters have {@code @Constraint} annotations.
	 * @since 6.1
	 */
	public void setMethodValidationApplicable(boolean methodValidationApplicable) {
		this.methodValidationApplicable = methodValidationApplicable;
	}


	/**
	 * Create a binder with a target object.
	 * @param exchange the current exchange
	 * @param target the object to create a data binder for
	 * @param name the name of the target object
	 * @return the created data binder
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, @Nullable Object target, String name) {
		return createDataBinder(exchange, target, name, null);
	}

	/**
	 * Shortcut method to create a binder without a target object.
	 * @param exchange the current exchange
	 * @param name the name of the target object
	 * @return the created data binder
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name, null);
	}

	/**
	 * Create a binder with a target object and a {@link ResolvableType targetType}.
	 * If the target is {@code null}, then
	 * {@link WebExchangeDataBinder#setTargetType targetType} is set.
	 * @since 6.1
	 */
	public WebExchangeDataBinder createDataBinder(
			ServerWebExchange exchange, @Nullable Object target, String name, @Nullable ResolvableType targetType) {

		WebExchangeDataBinder dataBinder = new ExtendedWebExchangeDataBinder(target, name);
		dataBinder.setNameResolver(new BindParamNameResolver());

		if (target == null && targetType != null) {
			dataBinder.setTargetType(targetType);
		}

		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}

		dataBinder = initDataBinder(dataBinder, exchange);

		if (this.methodValidationApplicable && targetType != null) {
			if (targetType.getSource() instanceof MethodParameter parameter) {
				MethodValidationInitializer.initBinder(dataBinder, parameter);
			}
		}

		return dataBinder;
	}

	/**
	 * Initialize the data binder instance for the given exchange.
	 * @throws ServerErrorException if {@code @InitBinder} method invocation fails
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder;
	}

	/**
	 * Invoked before rendering to add {@link BindingResult} attributes where
	 * necessary, and also to promote model attributes listed as
	 * {@code @SessionAttributes} to the session.
	 * @param exchange the current exchange
	 * @since 6.1
	 */
	public void updateModel(ServerWebExchange exchange) {
		Map<String, Object> model = getModel().asMap();
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if (isBindingCandidate(name, value)) {
				if (!model.containsKey(BindingResult.MODEL_KEY_PREFIX + name)) {
					WebExchangeDataBinder binder = createDataBinder(exchange, value, name);
					model.put(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
				}
			}
		}
	}

	private boolean isBindingCandidate(String name, @Nullable Object value) {
		return (!name.startsWith(BindingResult.MODEL_KEY_PREFIX) && value != null &&
				!value.getClass().isArray() && !(value instanceof Collection) && !(value instanceof Map) &&
				this.reactiveAdapterRegistry.getAdapter(null, value) == null &&
				!BeanUtils.isSimpleValueType(value.getClass()));
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
			return super.getValuesToBind(exchange).doOnNext(map ->
					map.putAll(exchange.<Map<String, String>>getAttributeOrDefault(
							HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap())));
		}
	}


	/**
	 * Excludes Bean Validation if the method parameter has {@code @Valid}.
	 */
	private static class MethodValidationInitializer {

		public static void initBinder(DataBinder binder, MethodParameter parameter) {
			if (ReactiveAdapterRegistry.getSharedInstance().getAdapter(parameter.getParameterType()) == null) {
				for (Annotation annotation : parameter.getParameterAnnotations()) {
					if (annotation.annotationType().getName().equals("jakarta.validation.Valid")) {
						binder.setExcludedValidators(validator -> validator instanceof jakarta.validation.Validator);
					}
				}
			}
		}
	}

}
