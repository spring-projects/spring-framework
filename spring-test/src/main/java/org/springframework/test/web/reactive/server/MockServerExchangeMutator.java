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

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Built-in {@link WebFilter} for applying {@code ServerWebExchange}
 * transformations during requests from the {@code WebTestClient} to a mock
 * server -- i.e. when one of the following is in use:
 * <ul>
 * <li>{@link WebTestClient#bindToController}
 * <li>{@link WebTestClient#bindToRouterFunction}
 * <li>{@link WebTestClient#bindToApplicationContext}
 * <li>{@link WebTestClient#bindToWebHandler}
 * </ul>
 *
 * <p>Example of registering a "global" transformation:
 * <pre class="code">
 *
 * MockServerExchangeMutator mutator = new MockServerExchangeMutator(exchange -> ...);
 * WebTestClient client = WebTestClient.bindToController(new MyController()).webFilter(mutator).build()
 * </pre>
 *
 * <p>Example of registering "per client" transformations:
 * <pre class="code">
 *
 * MockServerExchangeMutator mutator = new MockServerExchangeMutator(exchange -> ...);
 * WebTestClient client = WebTestClient.bindToController(new MyController()).webFilter(mutator).build()
 *
 * WebTestClient clientA = mutator.filterClient(client, exchange -> ...);
 * // Use client A...
 *
 * WebTestClient clientB = mutator.filterClient(client, exchange -> ...);
 * // Use client B...
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerExchangeMutator implements WebFilter {

	private final Function<ServerWebExchange, ServerWebExchange> mutator;

	private final Map<String, Function<ServerWebExchange, ServerWebExchange>> perRequestMutators =
			new ConcurrentHashMap<>(4);


	public MockServerExchangeMutator(Function<ServerWebExchange, ServerWebExchange> mutator) {
		Assert.notNull(mutator, "'mutator' is required");
		this.mutator = mutator;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return chain.filter(getMutatorsFor(exchange).apply(exchange));
	}

	private Function<ServerWebExchange, ServerWebExchange> getMutatorsFor(ServerWebExchange exchange) {
		String id = getRequestId(exchange.getRequest().getHeaders());
		Function<ServerWebExchange, ServerWebExchange> clientMutator = this.perRequestMutators.remove(id);
		return (clientMutator != null ? this.mutator.andThen(clientMutator) : this.mutator);
	}

	private String getRequestId(HttpHeaders headers) {
		String id = headers.getFirst(WebTestClient.WEBTESTCLIENT_REQUEST_ID);
		Assert.notNull(id, "No \"" + WebTestClient.WEBTESTCLIENT_REQUEST_ID + "\" header");
		return id;
	}


	/**
	 * Apply a filter to the given client in order to apply
	 * {@code ServerWebExchange} transformations only to requests executed
	 * through the returned client instance. See examples in the
	 * {@link MockServerExchangeMutator class-level Javadoc}.
	 *
	 * @param mutator the per-request mutator to use
	 * @param mutators additional per-request mutators to use
	 * @return a new client instance filtered with {@link WebTestClient#filter}
	 */
	@SafeVarargs
	public final WebTestClient filterClient(WebTestClient client,
			UnaryOperator<ServerWebExchange> mutator, UnaryOperator<ServerWebExchange>... mutators) {

		return client.filter((request, next) -> {
			String id = getRequestId(request.headers());
			registerPerRequestMutator(id, mutator);
			for (UnaryOperator<ServerWebExchange> current : mutators) {
				registerPerRequestMutator(id, current);
			}
			return next.exchange(request);
		});
	}

	private void registerPerRequestMutator(String id, UnaryOperator<ServerWebExchange> m) {
		this.perRequestMutators.compute(id, (s, value) -> value != null ? value.andThen(m) : m);
	}

}
