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

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;


/**
 * Apply {@code ServerWebExchange} transformations during "mock" server tests
 * with the {@code WebTestClient}.
 *
 * <p>Register the {@code WebFilter} while setting up the mock server through
 * one of the following:
 * <ul>
 * <li>{@link WebTestClient#bindToController}
 * <li>{@link WebTestClient#bindToRouterFunction}
 * <li>{@link WebTestClient#bindToApplicationContext}
 * <li>{@link WebTestClient#bindToWebHandler}
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre class="code">
 * Function&lt;ServerWebExchange, ServerWebExchange&gt; fn1 = ...;
 * Function&lt;ServerWebExchange, ServerWebExchange&gt; fn2 = ...;
 *
 * ExchangeMutatorWebFilter mutator = new ExchangeMutatorWebFilter(fn1().andThen(fn2()));
 * WebTestClient client = WebTestClient.bindToController(new MyController()).webFilter(mutator).build();
 * </pre>
 *
 *
 * <p>It is also possible to apply "per request" transformations:
 *
 * <pre class="code">
 * ExchangeMutatorWebFilter mutator = new ExchangeMutatorWebFilter();
 * WebTestClient client = WebTestClient.bindToController(new MyController()).webFilter(mutator).build();
 *
 * Function&lt;ServerWebExchange, ServerWebExchange&gt; fn1 = ...;
 * Function&lt;ServerWebExchange, ServerWebExchange&gt; fn2 = ...;
 *
 * client.filter(mutator.perClient(fn1().andThen(fn2()))).get().uri("/").exchange();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ExchangeMutatorWebFilter implements WebFilter {

	private final Function<ServerWebExchange, ServerWebExchange> processor;

	private final Map<String, Function<ServerWebExchange, ServerWebExchange>> perRequestProcessors =
			new ConcurrentHashMap<>(4);


	public ExchangeMutatorWebFilter() {
		this(exchange -> exchange);
	}

	public ExchangeMutatorWebFilter(Function<ServerWebExchange, ServerWebExchange> processor) {
		Assert.notNull(processor, "'processor' is required");
		this.processor = processor;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		exchange = getProcessor(exchange).apply(exchange);
		return chain.filter(exchange);
	}

	private Function<ServerWebExchange, ServerWebExchange> getProcessor(ServerWebExchange exchange) {
		String id = getRequestId(exchange.getRequest().getHeaders());
		Function<ServerWebExchange, ServerWebExchange> clientMutator = this.perRequestProcessors.remove(id);
		return (clientMutator != null ? this.processor.andThen(clientMutator) : this.processor);
	}

	private String getRequestId(HttpHeaders headers) {
		String id = headers.getFirst(WebTestClient.WEBTESTCLIENT_REQUEST_ID);
		Assert.notNull(id, "No \"" + WebTestClient.WEBTESTCLIENT_REQUEST_ID + "\" header");
		return id;
	}

	/**
	 * Apply the given processor only to requests performed through the client
	 * instance filtered with the returned filter. See class-level Javadoc for
	 * sample code.
	 * @param processor the exchange processor to use
	 * @return client filter for use with {@link WebTestClient#filter}
	 */
	public ExchangeFilterFunction perClient(Function<ServerWebExchange, ServerWebExchange> processor) {
		return (request, next) -> {
			String id = getRequestId(request.headers());
			this.perRequestProcessors.compute(id,
					(s, value) -> value != null ? value.andThen(processor) : processor);
			return next.exchange(request);
		};
	}

}
