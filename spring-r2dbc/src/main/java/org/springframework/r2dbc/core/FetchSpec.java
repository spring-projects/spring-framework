/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

/**
 * Union type for fetching results.
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> the row result type
 * @see RowsFetchSpec
 * @see UpdatedRowsFetchSpec
 */
public interface FetchSpec<T> extends RowsFetchSpec<T>, UpdatedRowsFetchSpec {

}
