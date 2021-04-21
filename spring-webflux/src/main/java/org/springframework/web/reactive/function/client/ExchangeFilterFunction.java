/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;

/**
 * Represents a function that filters an {@linkplain ExchangeFunction exchange function}.
 * <p>The filter is executed when a {@code Subscriber} subscribes to the
 * {@code Publisher} returned by the {@code WebClient}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFilterFunction {

	/**
	 * Apply this filter to the given request and exchange function.
	 * <p>The given {@linkplain ExchangeFunction} represents the next entity
	 * in the chain, to be invoked via
	 * {@linkplain ExchangeFunction#exchange(ClientRequest) invoked} in order to
	 * proceed with the exchange, or not invoked to shortcut the chain.
	 *
	 * <p><strong>Note:</strong> When a filter handles the response after the
	 * call to {@link ExchangeFunction#exchange}, extra care must be taken to
	 * always consume its content or otherwise propagate it downstream for
	 * further handling, for example by the {@link WebClient}. Please, see the
	 * reference documentation for more details on this.
	 *
	 * @param request the current request
	 * @param next the next exchange function in the chain
	 * @return the filtered response
	 */
	Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);

	/**
	 * Return a composed filter function that first applies this filter, and
	 * then applies the given {@code "after"} filter.
	 * @param afterFilter the filter to apply after this filter
	 * @return the composed filter
	 */
	default ExchangeFilterFunction andThen(ExchangeFilterFunction afterFilter) {
		Assert.notNull(afterFilter, "ExchangeFilterFunction must not be null");
		return (request, next) ->
				filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
	}

	/**
	 * Apply this filter to the given {@linkplain ExchangeFunction}, resulting
	 * in a filtered exchange function.
	 * @param exchange the exchange function to filter
	 * @return the filtered exchange function
	 */
	default ExchangeFunction apply(ExchangeFunction exchange) {
		Assert.notNull(exchange, "ExchangeFunction must not be null");
		return request -> this.filter(request, exchange);
	}

	/**
	 * Adapt the given request processor function to a filter function that only
	 * operates on the {@code ClientRequest}.
	 * @param processor the request processor
	 * @return the resulting filter adapter
	 */
	static ExchangeFilterFunction ofRequestProcessor(Function<ClientRequest, Mono<ClientRequest>> processor) {
		Assert.notNull(processor, "ClientRequest Function must not be null");
		return (request, next) -> processor.apply(request).flatMap(next::exchange);
	}

	/**
	 * Adapt the given response processor function to a filter function that
	 * only operates on the {@code ClientResponse}.
	 * @param processor the response processor
	 * @return the resulting filter adapter
	 */
	static ExchangeFilterFunction ofResponseProcessor(Function<ClientResponse, Mono<ClientResponse>> processor) {
		Assert.notNull(processor, "ClientResponse Function must not be null");
		return (request, next) -> next.exchange(request).flatMap(processor);
	}

}
