/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.function.Function;

import io.r2dbc.spi.Connection;

import org.springframework.lang.Nullable;

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

	@Nullable
	@Override
	public String getSql() {
		return this.sql.getSql();
	}
}
