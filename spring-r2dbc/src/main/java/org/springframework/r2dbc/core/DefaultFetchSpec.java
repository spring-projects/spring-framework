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
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * Default {@link FetchSpec} implementation.
 *
 * @author Mark Paluch
 * @author Simon Baslé
 * @since 5.3
 * @param <T> the row result type
 */
class DefaultFetchSpec<T> implements FetchSpec<T> {

	private final ConnectionAccessor connectionAccessor;

	private final ResultFunction resultFunction;

	private final Function<Connection, Mono<Long>> updatedRowsFunction;

	private final Function<Result, Publisher<T>> resultAdapter;


	DefaultFetchSpec(ConnectionAccessor connectionAccessor,
			ResultFunction resultFunction,
			Function<Connection, Mono<Long>> updatedRowsFunction,
			Function<Result, Publisher<T>> resultAdapter) {

		this.connectionAccessor = connectionAccessor;
		this.resultFunction = resultFunction;
		this.updatedRowsFunction = new DelegateConnectionFunction<>(resultFunction, updatedRowsFunction);
		this.resultAdapter = resultAdapter;
	}


	@Override
	public Mono<T> one() {
		return all().singleOrEmpty()
			.onErrorMap(IndexOutOfBoundsException.class, ex -> {
				String message = String.format("Query [%s] returned non unique result.", this.resultFunction.getSql());
				return new IncorrectResultSizeDataAccessException(message, 1);
			});
	}

	@Override
	public Mono<T> first() {
		return all().next();
	}

	@Override
	public Flux<T> all() {
		return this.connectionAccessor.inConnectionMany(new DelegateConnectionFunction<>(this.resultFunction,
				connection -> this.resultFunction.apply(connection)
						.flatMap(this.resultAdapter)));
	}

	@Override
	public Mono<Long> rowsUpdated() {
		return this.connectionAccessor.inConnection(this.updatedRowsFunction);
	}

}
