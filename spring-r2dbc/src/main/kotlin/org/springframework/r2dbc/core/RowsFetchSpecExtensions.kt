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
package org.springframework.r2dbc.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.dao.EmptyResultDataAccessException

/**
 * Non-nullable Coroutines variant of [RowsFetchSpec.one].
 *
 * @author Sebastien Deleuze
 */
suspend fun <T> RowsFetchSpec<T>.awaitOne(): T {
	return one().awaitFirstOrNull() ?: throw EmptyResultDataAccessException(1)
}

/**
 * Nullable Coroutines variant of [RowsFetchSpec.one].
 *
 * @author Sebastien Deleuze
 */
suspend fun <T> RowsFetchSpec<T>.awaitOneOrNull(): T? =
		one().awaitFirstOrNull()

/**
 * Non-nullable Coroutines variant of [RowsFetchSpec.first].
 *
 * @author Sebastien Deleuze
 */
suspend fun <T> RowsFetchSpec<T>.awaitFirst(): T {
	return first().awaitFirstOrNull() ?: throw EmptyResultDataAccessException(1)
}

/**
 * Nullable Coroutines variant of [RowsFetchSpec.first].
 *
 * @author Sebastien Deleuze
 */
suspend fun <T> RowsFetchSpec<T>.awaitFirstOrNull(): T? =
		first().awaitFirstOrNull()

/**
 * Coroutines [Flow] variant of [RowsFetchSpec.all].
 *
 * @author Sebastien Deleuze
 */
fun <T : Any> RowsFetchSpec<T>.flow(): Flow<T> = all().asFlow()
