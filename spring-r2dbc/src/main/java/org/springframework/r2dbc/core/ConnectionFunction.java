/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.function.Function;

import io.r2dbc.spi.Connection;

/**
 * Union type combining {@link Function} and {@link SqlProvider} to expose the SQL
 * that is related to the underlying action. The {@code SqlProvider} can support
 * lazy / generate-once semantics, in which case {@link #getSql()} can be {@code null}
 * until the {@code #apply(Connection)} method is invoked.
 *
 * @author Mark Paluch
 * @author Simon Baslé
 * @since 5.3
 * @param <R> the type of the result of the function.
 */
@SuppressWarnings("rawtypes")
sealed interface ConnectionFunction<R> extends Function<Connection, R>, SqlProvider
		permits DelegateConnectionFunction, ResultFunction {
}
