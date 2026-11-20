/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link ConnectionFunction} that produces a {@code Flux} of {@link Result} and that
 * defers generation of the SQL until the function has been applied.
 * Beforehand, the {@code getSql()} method simply returns {@code null}. The sql String is
 * also memoized during application, so that subsequent calls to {@link #getSql()} return
 * the same {@code String} without further calls to the {@code Supplier}.
 *
 * @author Mark Paluch
 * @author Simon Baslé
 * @since 5.3.26
 */
final class ResultFunction implements ConnectionFunction<Flux<Result>> {

	final Supplier<String> sqlSupplier;
	final BiFunction<Connection, String, Statement> statementFunction;
	final StatementFilterFunction filterFunction;
	final ExecuteFunction executeFunction;

	@Nullable String resolvedSql = null;

	ResultFunction(Supplier<String> sqlSupplier, BiFunction<Connection, String, Statement> statementFunction, StatementFilterFunction filterFunction, ExecuteFunction executeFunction) {
		this.sqlSupplier = sqlSupplier;
		this.statementFunction = statementFunction;
		this.filterFunction = filterFunction;
		this.executeFunction = executeFunction;
	}

	@Override
	public Flux<Result> apply(Connection connection) {
		String sql = this.sqlSupplier.get();
		Assert.state(StringUtils.hasText(sql), "SQL returned by supplier must not be empty");
		this.resolvedSql = sql;
		Statement statement = this.statementFunction.apply(connection, sql);
		return Flux.from(this.filterFunction.filter(statement, this.executeFunction))
				.cast(Result.class).checkpoint("SQL \"" + sql + "\" [DatabaseClient]");
	}

	@Override
	public @Nullable String getSql() {
		return this.resolvedSql;
	}
}
