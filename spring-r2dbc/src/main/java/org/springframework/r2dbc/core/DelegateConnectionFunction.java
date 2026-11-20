/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.function.Function;

import io.r2dbc.spi.Connection;
import org.jspecify.annotations.Nullable;

/**
 * A {@link ConnectionFunction} that delegates to a {@code SqlProvider} and a plain
 * {@code Function}.
 *
 * @author Simon Baslé
 * @since 5.3.26
 * @param <R> the type of the result of the function.
 */
final class DelegateConnectionFunction<R> implements ConnectionFunction<R> {

	private final SqlProvider sql;

	private final Function<Connection, R> function;


	DelegateConnectionFunction(SqlProvider sql, Function<Connection, R> function) {
		this.sql = sql;
		this.function = function;
	}


	@Override
	public R apply(Connection t) {
		return this.function.apply(t);
	}

	@Override
	public @Nullable String getSql() {
		return this.sql.getSql();
	}
}
