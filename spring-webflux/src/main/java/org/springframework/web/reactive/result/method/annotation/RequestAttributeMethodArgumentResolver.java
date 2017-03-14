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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves method arguments annotated with an @{@link RequestAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see SessionAttributeMethodArgumentResolver
 */
public class RequestAttributeMethodArgumentResolver extends AbstractNamedValueSyncArgumentResolver {


	/**
	 * @param beanFactory a bean factory to use for resolving  ${...}
	 * placeholder and #{...} SpEL expressions in default values;
	 * or {@code null} if default values are not expected to have expressions
	 * @param adapterRegistry for checking reactive type wrappers
	 */
	public RequestAttributeMethodArgumentResolver(ConfigurableBeanFactory beanFactory,
			ReactiveAdapterRegistry adapterRegistry) {

		super(beanFactory, adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter param) {
		return checkAnnotatedParamNoReactiveWrapper(param, RequestAttribute.class, (annot, type) -> true);
	}


	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		RequestAttribute annot = parameter.getParameterAnnotation(RequestAttribute.class);
		return new NamedValueInfo(annot.name(), annot.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	protected Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		return exchange.getAttribute(name);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing request attribute '" + name + "' of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}

}
