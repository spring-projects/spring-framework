/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Contract for fetching tabular results.
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> the row result type
 */
public interface RowsFetchSpec<T> {

	/**
	 * Get exactly zero or one result.
	 *
	 * @return a mono emitting one element. {@link Mono#empty()} if no match found.
	 * Completes with {@code IncorrectResultSizeDataAccessException} if more than one match found
	 */
	Mono<T> one();

	/**
	 * Get the first or no result.
	 * @return a mono emitting the first element. {@link Mono#empty()} if no match found
	 */
	Mono<T> first();

	/**
	 * Get all matching elements.
	 * @return a flux emitting all results
	 */
	Flux<T> all();

}
