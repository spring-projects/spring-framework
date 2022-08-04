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

import java.util.function.BiFunction;
import java.util.function.Function;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * Default {@link FetchSpec} implementation.
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> the row result type
 */
class DefaultFetchSpec<T> implements FetchSpec<T> {

	private final ConnectionAccessor connectionAccessor;

	private final String sql;

	private final Function<Connection, Flux<Result>> resultFunction;

	private final Function<Connection, Mono<Integer>> updatedRowsFunction;

	private final BiFunction<Row, RowMetadata, T> mappingFunction;


	DefaultFetchSpec(ConnectionAccessor connectionAccessor, String sql,
			Function<Connection, Flux<Result>> resultFunction,
			Function<Connection, Mono<Integer>> updatedRowsFunction,
			BiFunction<Row, RowMetadata, T> mappingFunction) {

		this.sql = sql;
		this.connectionAccessor = connectionAccessor;
		this.resultFunction = resultFunction;
		this.updatedRowsFunction = updatedRowsFunction;
		this.mappingFunction = mappingFunction;
	}


	@Override
	public Mono<T> one() {
		return all().buffer(2)
				.flatMap(list -> {
					if (list.isEmpty()) {
						return Mono.empty();
					}
					if (list.size() > 1) {
						return Mono.error(new IncorrectResultSizeDataAccessException(
								String.format("Query [%s] returned non unique result.", this.sql),
								1));
					}
					return Mono.just(list.get(0));
				}).next();
	}

	@Override
	public Mono<T> first() {
		return all().next();
	}

	@Override
	public Flux<T> all() {
		return this.connectionAccessor.inConnectionMany(new ConnectionFunction<>(this.sql,
				connection -> this.resultFunction.apply(connection)
						.flatMap(result -> result.map(this.mappingFunction))));
	}

	@Override
	public Mono<Integer> rowsUpdated() {
		return this.connectionAccessor.inConnection(this.updatedRowsFunction);
	}

}
