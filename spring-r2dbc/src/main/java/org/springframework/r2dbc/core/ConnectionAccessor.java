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

import java.util.function.Function;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.dao.DataAccessException;

/**
 * Interface declaring methods that accept callback {@link Function}
 * to operate within the scope of a {@link Connection}.
 * Callback functions operate on a provided connection and must not
 * close the connection as the connections may be pooled or be
 * subject to other kinds of resource management.
 *
 * <p> Callback functions are responsible for creating a
 * {@link org.reactivestreams.Publisher} that defines the scope of how
 * long the allocated {@link Connection} is valid. Connections are
 * released after the publisher terminates.
 *
 * @author Mark Paluch
 * @since 5.3
 */
public interface ConnectionAccessor {

	/**
	 * Execute a callback {@link Function} within a {@link Connection} scope.
	 * The function is responsible for creating a {@link Mono}. The connection
	 * is released after the {@link Mono} terminates (or the subscription
	 * is cancelled). Connection resources must not be passed outside the
	 * {@link Function} closure, otherwise resources may get defunct.
	 * @param action the callback object that specifies the connection action
	 * @return the resulting {@link Mono}
	 */
	<T> Mono<T> inConnection(Function<Connection, Mono<T>> action) throws DataAccessException;

	/**
	 * Execute a callback {@link Function} within a {@link Connection} scope.
	 * The function is responsible for creating a {@link Flux}. The connection
	 * is released after the {@link Flux} terminates (or the subscription
	 * is cancelled). Connection resources must not be passed outside the
	 * {@link Function} closure, otherwise resources may get defunct.
	 * @param action the callback object that specifies the connection action
	 * @return the resulting {@link Flux}
	 */
	<T> Flux<T> inConnectionMany(Function<Connection, Flux<T>> action) throws DataAccessException;

}
