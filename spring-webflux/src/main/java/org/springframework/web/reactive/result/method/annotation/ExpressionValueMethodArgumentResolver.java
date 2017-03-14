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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method arguments annotated with {@code @Value}.
 *
 * <p>An {@code @Value} does not have a name but gets resolved from the default
 * value string, which may contain ${...} placeholder or Spring Expression
 * Language #{...} expressions.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExpressionValueMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {


	/**
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to contain expressions
	 * @param adapterRegistry for checking reactive type wrappers
	 */
	public ExpressionValueMethodArgumentResolver(ConfigurableBeanFactory beanFactory,
			ReactiveAdapterRegistry adapterRegistry) {

		super(beanFactory, adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, Value.class, (annot, type) -> true);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		Value annotation = parameter.getParameterAnnotation(Value.class);
		return new ExpressionValueNamedValueInfo(annotation);
	}

	@Override
	protected Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		// No name to resolve
		return Optional.empty();
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		throw new UnsupportedOperationException("@Value is never required: " + parameter.getMethod());
	}


	private static class ExpressionValueNamedValueInfo extends NamedValueInfo {

		private ExpressionValueNamedValueInfo(Value annotation) {
			super("@Value", false, annotation.value());
		}
	}

}
