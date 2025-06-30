/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * Resolves method argument value of type {@link WebSession}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see ServerWebExchangeMethodArgumentResolver
 */
public class WebSessionMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	// We need this resolver separate from ServerWebExchangeArgumentResolver which
	// implements SyncHandlerMethodArgumentResolver.


	public WebSessionMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterType(parameter, WebSession.class::isAssignableFrom);
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Mono<WebSession> session = exchange.getSession();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		return (adapter != null ? Mono.just(adapter.fromPublisher(session)) : Mono.from(session));
	}

}
