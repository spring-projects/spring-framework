/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * @return a Mono emitting one element, or {@link Mono#empty()} if no match found.
	 * Completes with {@code IncorrectResultSizeDataAccessException} if more than one match found
	 */
	Mono<T> one();

	/**
	 * Get the first or no result.
	 * @return a Mono emitting the first element, or {@link Mono#empty()} if no match found
	 */
	Mono<T> first();

	/**
	 * Get all matching elements.
	 * @return a Flux emitting all results
	 */
	Flux<T> all();

}
