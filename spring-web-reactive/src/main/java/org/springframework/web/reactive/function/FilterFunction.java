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

import org.springframework.web.reactive.function.support.RequestWrapper;

/**
 * Represents a function that filters a {@linkplain HandlerFunction handler function}.
 *
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
 * @param <R> the type of the response of the function
 * @author Arjen Poutsma
 * @since 5.0
 * @see RouterFunction#filter(FilterFunction) 
 */
@FunctionalInterface
public interface FilterFunction<T, R> {

	/**
	 * Apply this filter to the given handler function. The given
	 * {@linkplain HandlerFunction handler function} represents the next entity in the
	 * chain, and can be {@linkplain HandlerFunction#handle(Request) invoked} in order
	 * to proceed to this entity, or not invoked to block the chain.
	 *
	 * @param request the request
	 * @param next    the next handler or filter function in the chain
	 * @return the filtered response
	 * @see RequestWrapper
	 */
	Response<R> filter(Request request, HandlerFunction<T> next);

}
