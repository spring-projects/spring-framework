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
