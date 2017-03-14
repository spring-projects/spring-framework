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

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

/**
 * Resolves method arguments annotated with an @{@link SessionAttribute}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see RequestAttributeMethodArgumentResolver
 */
public class SessionAttributeMethodArgumentResolver extends AbstractNamedValueArgumentResolver {


	public SessionAttributeMethodArgumentResolver(ConfigurableBeanFactory beanFactory,
			ReactiveAdapterRegistry adapterRegistry) {

		super(beanFactory, adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(SessionAttribute.class);
	}

	@Override
	protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
		SessionAttribute annot = parameter.getParameterAnnotation(SessionAttribute.class);
		return new NamedValueInfo(annot.name(), annot.required(), ValueConstants.DEFAULT_NONE);
	}

	@Override
	protected Mono<Object> resolveName(String name, MethodParameter parameter,
			ServerWebExchange exchange) {

		return exchange.getSession()
				.map(session -> session.getAttribute(name))
				.filter(Optional::isPresent)
				.map(Optional::get);
	}

	@Override
	protected void handleMissingValue(String name, MethodParameter parameter) {
		String type = parameter.getNestedParameterType().getSimpleName();
		String reason = "Missing session attribute '" + name + "' of type " + type;
		throw new ServerWebInputException(reason, parameter);
	}

}
