/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.EmptyResultDataAccessException
import reactor.core.publisher.Mono

/**
 * Non-nullable Coroutines variant of [RowsFetchSpec.one].
 *
 * @throws EmptyResultDataAccessException if the underlying [Mono] does not emit any value
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> RowsFetchSpec<T>.awaitOne(): T {
	return one().awaitSingleOrNull() ?: throw EmptyResultDataAccessException(1)
}

/**
 * Nullable Coroutines variant of [RowsFetchSpec.one].
 *
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> RowsFetchSpec<T>.awaitOneOrNull(): T? =
		one().awaitSingleOrNull()

/**
 * Non-nullable Coroutines variant of [RowsFetchSpec.first].
 *
 * @throws EmptyResultDataAccessException if the underlying [Mono] does not emit any value
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> RowsFetchSpec<T>.awaitSingle(): T {
	return first().awaitSingleOrNull() ?: throw EmptyResultDataAccessException(1)
}

/**
 * Nullable Coroutines variant of [RowsFetchSpec.first].
 *
 * @author Sebastien Deleuze
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> RowsFetchSpec<T>.awaitSingleOrNull(): T? =
		first().awaitSingleOrNull()

/**
 * Coroutines [Flow] variant of [RowsFetchSpec.all].
 *
 * @author Sebastien Deleuze
 */
fun <T : Any> RowsFetchSpec<T>.flow(): Flow<T> = all().asFlow()
