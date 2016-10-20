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

import java.util.Optional;

/**
 * Represents a function that routes to a {@linkplain HandlerFunction handler function}.
 *
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to route to
 * @author Arjen Poutsma
 * @since 5.0
 * @see RouterFunctions
 */
@FunctionalInterface
public interface RouterFunction<T> {

	/**
	 * Return the {@linkplain HandlerFunction handler function} that matches the given request.
	 * @param request the request to route to
	 * @return an {@code Optional} describing the {@code HandlerFunction} that matches this request,
	 * or an empty {@code Optional} if there is no match
	 */
	Optional<HandlerFunction<T>> route(ServerRequest request);

	/**
	 * Return a composed routing function that first invokes this function,
	 * and then invokes the {@code other} function (of the same type {@code T}) if this route had
	 * {@linkplain Optional#empty() no result}.
	 *
	 * @param other the function of type {@code T} to apply when this function has no result
	 * @return a composed function that first routes with this function and then the {@code other} function if this
	 * function has no result
	 */
	default RouterFunction<T> andSame(RouterFunction<T> other) {
		return request -> {
			Optional<HandlerFunction<T>> result = this.route(request);
			return result.isPresent() ? result : other.route(request);
		};
	}

	/**
	 * Return a composed routing function that first invokes this function,
	 * and then invokes the {@code other} function (of a different type) if this route had
	 * {@linkplain Optional#empty() no result}.
	 *
	 * @param other the function to apply when this function has no result
	 * @return a composed function that first routes with this function and then the {@code other} function if this
	 * function has no result
	 */
	default RouterFunction<?> and(RouterFunction<?> other) {
		return request -> {
			Optional<HandlerFunction<Object>> result = this.route(request).
					map(RouterFunctions::cast);
			return result.isPresent() ? result : other.route(request)
					.map(RouterFunctions::cast);
		};
	}

	/**
	 * Filter all {@linkplain HandlerFunction handler functions} routed by this function with the given
	 * {@linkplain HandlerFilterFunction filter function}.
	 *
	 * @param filterFunction the filter to apply
	 * @param <S>            the filter return type
	 * @return the filtered routing function
	 */
	default <S> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
		return request -> this.route(request)
				.map(handlerFunction -> filterRequest -> filterFunction.filter(filterRequest, handlerFunction));
	}

}
