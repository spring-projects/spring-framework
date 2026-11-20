/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import reactor.core.publisher.Mono;

/**
 * Contract for fetching the number of affected rows.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public interface UpdatedRowsFetchSpec {

	/**
	 * Get the number of updated rows.
	 * @return a Mono emitting the number of updated rows
	 */
	Mono<Long> rowsUpdated();

}
