/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Exposes request-response exchange functionality, such as to
 * {@linkplain #create(ClientHttpConnector) create} an {@code ExchangeFunction} given a
 * {@code ClientHttpConnector}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class ExchangeFunctions {

	private static final Log logger = LogFactory.getLog(ExchangeFunctions.class);


	/**
	 * Create a new {@link ExchangeFunction} with the given connector. This method uses
	 * {@linkplain ExchangeStrategies#withDefaults() default strategies}.
	 * @param connector the connector to create connections
	 * @return the created function
	 */
	public static ExchangeFunction create(ClientHttpConnector connector) {
		return create(connector, ExchangeStrategies.withDefaults());
	}

	/**
	 * Create a new {@link ExchangeFunction} with the given connector and strategies.
	 * @param connector the connector to create connections
	 * @param strategies the strategies to use
	 * @return the created function
	 */
	public static ExchangeFunction create(ClientHttpConnector connector, ExchangeStrategies strategies) {
		Assert.notNull(connector, "ClientHttpConnector must not be null");
		Assert.notNull(strategies, "ExchangeStrategies must not be null");
		return new DefaultExchangeFunction(connector, strategies);
	}


	private static class DefaultExchangeFunction implements ExchangeFunction {

		private final ClientHttpConnector connector;

		private final ExchangeStrategies strategies;

		public DefaultExchangeFunction(ClientHttpConnector connector, ExchangeStrategies strategies) {
			this.connector = connector;
			this.strategies = strategies;
		}

		@Override
		public Mono<ClientResponse> exchange(ClientRequest request) {
			Assert.notNull(request, "ClientRequest must not be null");
			return this.connector
					.connect(request.method(), request.url(),
							clientHttpRequest -> request.writeTo(clientHttpRequest, this.strategies))
					.doOnSubscribe(subscription -> logger.debug("Subscriber present"))
					.doOnRequest(n -> logger.debug("Demand signaled"))
					.doOnCancel(() -> logger.debug("Cancelling request"))
					.map(response -> {
						if (logger.isDebugEnabled()) {
							int status = response.getRawStatusCode();
							HttpStatus resolvedStatus = HttpStatus.resolve(status);
							logger.debug("Response received, status: " + status +
									(resolvedStatus != null ? " " + resolvedStatus.getReasonPhrase() : ""));
						}
						return new DefaultClientResponse(response, this.strategies);
					});
		}
	}

}
