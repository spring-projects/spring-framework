/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.security.Principal;

import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolverSupport;
import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves method argument value of type {@link java.security.Principal}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see ServerWebExchangeMethodArgumentResolver
 */
public class PrincipalMethodArgumentResolver extends HandlerMethodArgumentResolverSupport {

	public PrincipalMethodArgumentResolver(ReactiveAdapterRegistry adapterRegistry) {
		super(adapterRegistry);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return checkParameterType(parameter, Principal.class::isAssignableFrom);
	}

	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		Mono<Principal> principal = exchange.getPrincipal();
		ReactiveAdapter adapter = getAdapterRegistry().getAdapter(parameter.getParameterType());
		return (adapter != null ? Mono.just(adapter.fromPublisher(principal)) : Mono.from(principal));
	}

}
