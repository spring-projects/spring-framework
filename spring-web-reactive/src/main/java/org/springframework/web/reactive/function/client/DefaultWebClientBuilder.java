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

package org.springframework.web.reactive.function.client;

import java.util.logging.Level;

import reactor.core.publisher.Mono;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link WebClient.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultWebClientBuilder implements WebClient.Builder {

	private ClientHttpConnector clientHttpConnector;

	private WebClientStrategies strategies = WebClientStrategies.withDefaults();

	private ExchangeFilterFunction filter = new NoOpFilter();


	public DefaultWebClientBuilder(ClientHttpConnector clientHttpConnector) {
		this.clientHttpConnector = clientHttpConnector;
	}

	@Override
	public WebClient.Builder strategies(WebClientStrategies strategies) {
		Assert.notNull(strategies, "'strategies' must not be null");
		this.strategies = strategies;
		return this;
	}

	@Override
	public WebClient.Builder filter(ExchangeFilterFunction filter) {
		Assert.notNull(filter, "'filter' must not be null");
		this.filter = filter.andThen(this.filter);
		return this;
	}

	@Override
	public WebClient build() {
		return new DefaultWebClient(this.clientHttpConnector, this.strategies, this.filter);
	}

	private final static class DefaultWebClient implements WebClient {

		private final ClientHttpConnector clientHttpConnector;

		private final WebClientStrategies strategies;

		private final ExchangeFilterFunction filter;

		public DefaultWebClient(
				ClientHttpConnector clientHttpConnector,
				WebClientStrategies strategies,
				ExchangeFilterFunction filter) {
			this.clientHttpConnector = clientHttpConnector;
			this.strategies = strategies;
			this.filter = filter;
		}

		@Override
		public Mono<ClientResponse> exchange(ClientRequest<?> request) {
			Assert.notNull(request, "'request' must not be null");

			return this.filter.filter(request, this::exchangeInternal);
		}

		private Mono<ClientResponse> exchangeInternal(ClientRequest<?> request) {
			return this.clientHttpConnector
					.connect(request.method(), request.url(),
							clientHttpRequest -> request
									.writeTo(clientHttpRequest, this.strategies))
					.log("org.springframework.web.client.reactive", Level.FINE)
					.map(clientHttpResponse -> new DefaultClientResponse(clientHttpResponse,
							this.strategies));
		}

		@Override
		public WebClient filter(ExchangeFilterFunction filter) {
			Assert.notNull(filter, "'filter' must not be null");

			ExchangeFilterFunction composedFilter = filter.andThen(this.filter);

			return new DefaultWebClient(this.clientHttpConnector, this.strategies, composedFilter);
		}
	}

	private class NoOpFilter implements ExchangeFilterFunction {

		@Override
		public Mono<ClientResponse> filter(ClientRequest<?> request, ExchangeFunction next) {
			return next.exchange(request);
		}
	}

}
