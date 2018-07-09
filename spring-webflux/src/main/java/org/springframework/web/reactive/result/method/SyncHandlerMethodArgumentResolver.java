/*
 * Copyright 2002-2018 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

/**
 * An extension of {@link HandlerMethodArgumentResolver} for implementations
 * that are synchronous in nature and do not block to resolve values.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface SyncHandlerMethodArgumentResolver extends HandlerMethodArgumentResolver {

	/**
	 * {@inheritDoc}
	 * <p>By default this simply delegates to {@link #resolveArgumentValue} for
	 * synchronous resolution.
	 */
	@Override
	default Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		return Mono.justOrEmpty(resolveArgumentValue(parameter, bindingContext, exchange));
	}

	/**
	 * Resolve the value for the method parameter synchronously.
	 * @param parameter the method parameter
	 * @param bindingContext the binding context to use
	 * @param exchange the current exchange
	 * @return the resolved value, if any
	 */
	@Nullable
	Object resolveArgumentValue(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange);

}
