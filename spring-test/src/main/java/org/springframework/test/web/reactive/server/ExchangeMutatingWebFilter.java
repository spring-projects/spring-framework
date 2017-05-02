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

package org.springframework.test.web.reactive.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * WebFilter for applying global and per-request transformations to a
 * {@link ServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ExchangeMutatingWebFilter implements WebFilter {

	private static final Function<ServerWebExchange, ServerWebExchange> NO_OP_MUTATOR = e -> e;


	private volatile Function<ServerWebExchange, ServerWebExchange> globalMutator = NO_OP_MUTATOR;

	private final Map<String, Function<ServerWebExchange, ServerWebExchange>> perRequestMutators =
			new ConcurrentHashMap<>(4);


	/**
	 * Register a global transformation function to apply to all requests.
	 * @param mutator the transformation function
	 */
	public void registerGlobalMutator(UnaryOperator<ServerWebExchange> mutator) {
		Assert.notNull(mutator, "'mutator' is required");
		this.globalMutator = this.globalMutator.andThen(mutator);
	}

	/**
	 * Register a per-request transformation function.
	 * @param requestId the "request-id" header value identifying the request
	 * @param mutator the transformation function
	 */
	public void registerPerRequestMutator(String requestId, UnaryOperator<ServerWebExchange> mutator) {
		this.perRequestMutators.compute(requestId,
				(s, value) -> value != null ? value.andThen(mutator) : mutator);
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		exchange = this.globalMutator.apply(exchange);
		exchange = getMutatorFor(exchange).apply(exchange);
		return chain.filter(exchange);
	}

	private Function<ServerWebExchange, ServerWebExchange> getMutatorFor(ServerWebExchange exchange) {
		String id = WiretapConnector.getRequestId(exchange.getRequest().getHeaders());
		Function<ServerWebExchange, ServerWebExchange> mutator = this.perRequestMutators.remove(id);
		return mutator != null ? mutator : NO_OP_MUTATOR;
	}

}
