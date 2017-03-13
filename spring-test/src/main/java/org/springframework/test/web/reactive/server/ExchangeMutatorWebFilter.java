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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * WebFilter that applies global and request-specific transformation on
 * {@link ServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ExchangeMutatorWebFilter implements WebFilter {

	private volatile List<UnaryOperator<ServerWebExchange>> globalMutators = new ArrayList<>(4);

	private final Map<String, UnaryOperator<ServerWebExchange>> requestMutators = new ConcurrentHashMap<>(4);


	/**
	 * Register a global transformation function to apply to all requests.
	 * @param mutator the transformation function
	 */
	public void register(UnaryOperator<ServerWebExchange> mutator) {
		Assert.notNull(mutator, "'mutator' is required");
		this.globalMutators.add(mutator);
	}

	/**
	 * Register a per-request transformation function.
	 * @param requestId the "request-id" header value identifying the request
	 * @param mutator the transformation function
	 */
	public void register(String requestId, UnaryOperator<ServerWebExchange> mutator) {
		this.requestMutators.put(requestId, mutator);
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		for (UnaryOperator<ServerWebExchange> mutator : this.globalMutators) {
			exchange = mutator.apply(exchange);
		}

		String requestId = WiretapConnector.getRequestId(exchange.getRequest().getHeaders());
		UnaryOperator<ServerWebExchange> mutator = this.requestMutators.remove(requestId);
		if (mutator != null) {
			exchange = mutator.apply(exchange);
		}

		return chain.filter(exchange);
	}

}
