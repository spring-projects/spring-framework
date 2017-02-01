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
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * An extension of {@link AbstractNamedValueArgumentResolver} for named value
 * resolvers that are synchronous and yet non-blocking. Sub-classes implement
 * the synchronous {@link #resolveNamedValue} to which the asynchronous
 * {@link #resolveName} delegates to by default.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractNamedValueSyncArgumentResolver extends AbstractNamedValueArgumentResolver
		implements SyncHandlerMethodArgumentResolver {

	public AbstractNamedValueSyncArgumentResolver(ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	@Override
	public Optional<Object> resolveArgumentValue(MethodParameter parameter,
			BindingContext bindingContext, ServerWebExchange exchange) {

		// This will not block
		Object value = resolveArgument(parameter, bindingContext, exchange).block();
		return Optional.ofNullable(value);
	}

	@Override
	protected Mono<Object> resolveName(String name, MethodParameter parameter, ServerWebExchange exchange) {
		return Mono.justOrEmpty(resolveNamedValue(name, parameter, exchange));
	}

	/**
	 * An abstract method for synchronous resolution of method argument values
	 * that sub-classes must implement.
	 * @param name the name of the value being resolved
	 * @param parameter the method parameter to resolve to an argument value
	 * (pre-nested in case of a {@link java.util.Optional} declaration)
	 * @param exchange the current exchange
	 * @return the resolved argument value, if any
	 */
	protected abstract Optional<Object> resolveNamedValue(String name, MethodParameter parameter,
			ServerWebExchange exchange);

}
