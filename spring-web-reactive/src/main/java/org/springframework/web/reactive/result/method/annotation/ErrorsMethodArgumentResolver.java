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
package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolve {@link Errors} or {@link BindingResult} method arguments.
 * An {@code Errors} argument is expected to appear immediately after the
 * model attribute in the method signature.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ErrorsMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final ReactiveAdapterRegistry adapterRegistry;


	/**
	 * Class constructor.
	 * @param registry for adapting to other reactive types from and to Mono
	 */
	public ErrorsMethodArgumentResolver(ReactiveAdapterRegistry registry) {
		Assert.notNull(registry, "'ReactiveAdapterRegistry' is required.");
		this.adapterRegistry = registry;
	}


	/**
	 * Return the configured {@link ReactiveAdapterRegistry}.
	 */
	public ReactiveAdapterRegistry getAdapterRegistry() {
		return this.adapterRegistry;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> clazz = parameter.getParameterType();
		return Errors.class.isAssignableFrom(clazz);
	}


	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext context,
			ServerWebExchange exchange) {

		String name = getModelAttributeName(parameter);
		Object errors = context.getModel().asMap().get(BindingResult.MODEL_KEY_PREFIX + name);

		Mono<?> errorsMono;
		if (Mono.class.isAssignableFrom(errors.getClass())) {
			errorsMono = (Mono<?>) errors;
		}
		else if (Errors.class.isAssignableFrom(errors.getClass())) {
			errorsMono = Mono.just(errors);
		}
		else {
			throw new IllegalStateException(
					"Unexpected Errors/BindingResult type: " + errors.getClass().getName());
		}

		return errorsMono.cast(Object.class);
	}

	private String getModelAttributeName(MethodParameter parameter) {

		Assert.isTrue(parameter.getParameterIndex() > 0,
				"Errors argument must be immediately after a model attribute argument.");

		int index = parameter.getParameterIndex() - 1;
		MethodParameter attributeParam = new MethodParameter(parameter.getMethod(), index);
		Class<?> attributeType = attributeParam.getParameterType();

		ResolvableType type = ResolvableType.forMethodParameter(attributeParam);
		ReactiveAdapter adapterTo = getAdapterRegistry().getAdapterTo(type.resolve());

		Assert.isNull(adapterTo, "Errors/BindingResult cannot be used with an async model attribute. " +
				"Either declare the model attribute without the async wrapper type " +
				"or handle WebExchangeBindException through the async type.");

		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		if (annot != null && StringUtils.hasText(annot.value())) {
			return annot.value();
		}
		// TODO: Conventions does not deal with async wrappers
		return ClassUtils.getShortNameAsProperty(attributeType);
	}

}
