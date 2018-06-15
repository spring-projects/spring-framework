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

package org.springframework.web.reactive.function.client;

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * Represents a function that filters an {@linkplain ExchangeFunction exchange function}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFilterFunction {

	/**
	 * Apply this filter to the given request and exchange function.
	 * <p>The given {@linkplain ExchangeFunction exchange function} represents the next entity
	 * in the chain, and can be {@linkplain ExchangeFunction#exchange(ClientRequest) invoked}
	 * in order to proceed to the exchange, or not invoked to block the chain.
	 * @param request the request
	 * @param next the next exchange function in the chain
	 * @return the filtered response
	 */
	Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);

	/**
	 * Return a composed filter function that first applies this filter, and then applies the
	 * {@code after} filter.
	 * @param after the filter to apply after this filter is applied
	 * @return a composed filter that first applies this function and then applies the
	 * {@code after} function
	 */
	default ExchangeFilterFunction andThen(ExchangeFilterFunction after) {
		Assert.notNull(after, "ExchangeFilterFunction must not be null");
		return (request, next) -> {
			ExchangeFunction nextExchange = exchangeRequest -> after.filter(exchangeRequest, next);
			return filter(request, nextExchange);
		};
	}

	/**
	 * Apply this filter to the given exchange function, resulting in a filtered exchange function.
	 * @param exchange the exchange function to filter
	 * @return the filtered exchange function
	 */
	default ExchangeFunction apply(ExchangeFunction exchange) {
		Assert.notNull(exchange, "ExchangeFunction must not be null");
		return request -> this.filter(request, exchange);
	}

	/**
	 * Adapt the given request processor function to a filter function that only operates on the
	 * {@code ClientRequest}.
	 * @param requestProcessor the request processor
	 * @return the filter adaptation of the request processor
	 */
	static ExchangeFilterFunction ofRequestProcessor(Function<ClientRequest, Mono<ClientRequest>> requestProcessor) {
		Assert.notNull(requestProcessor, "Function must not be null");
		return (request, next) -> requestProcessor.apply(request).flatMap(next::exchange);
	}

	/**
	 * Adapt the given response processor function to a filter function that only operates on the
	 * {@code ClientResponse}.
	 * @param responseProcessor the response processor
	 * @return the filter adaptation of the request processor
	 */
	static ExchangeFilterFunction ofResponseProcessor(Function<ClientResponse, Mono<ClientResponse>> responseProcessor) {
		Assert.notNull(responseProcessor, "Function must not be null");
		return (request, next) -> next.exchange(request).flatMap(responseProcessor);
	}

}
