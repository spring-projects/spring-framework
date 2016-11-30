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

package org.springframework.web.reactive.function;

import org.springframework.util.Assert;
import org.springframework.web.reactive.function.support.ServerRequestWrapper;

/**
 * Represents a function that filters a {@linkplain HandlerFunction handler function}.
 *
 * @param <T> the type of the response of the function
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 * @see RouterFunction#filter(HandlerFilterFunction)
 */
@FunctionalInterface
public interface HandlerFilterFunction<T> {

	/**
	 * Apply this filter to the given handler function. The given
	 * {@linkplain HandlerFunction handler function} represents the next entity in the
	 * chain, and can be {@linkplain HandlerFunction#handle(ServerRequest) invoked} in order
	 * to proceed to this entity, or not invoked to block the chain.
	 *
	 * @param request the request
	 * @param next    the next handler or filter function in the chain
	 * @return the filtered response
	 * @see ServerRequestWrapper
	 */
	ServerResponse<T> filter(ServerRequest request, HandlerFunction<?> next);

	/**
	 * Return a composed filter function that first applies this filter, and then applies the
	 * {@code after} filter.
	 * @param after the filter to apply after this filter is applied
	 * @return a composed filter that first applies this function and then applies the
	 * {@code after} function
	 */
	default HandlerFilterFunction<T> andThen(HandlerFilterFunction<T> after) {
		Assert.notNull(after, "'after' must not be null");
		return (request, next) -> {
			HandlerFunction<T> nextHandler =
					handlerRequest -> after.filter(handlerRequest, next);
			return filter(request, nextHandler);
		};
	}

	/**
	 * Apply this filter to the given handler function, resulting in a filtered handler function.
	 * @param handler the handler function to filter
	 * @return the filtered handler function
	 */
	default HandlerFunction<T> apply(HandlerFunction<?> handler) {
		Assert.notNull(handler, "'handler' must not be null");
		return request -> this.filter(request, handler);
	}

}
