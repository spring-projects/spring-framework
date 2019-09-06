package org.springframework.transaction.reactive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono

/**
 * Coroutines variant of [TransactionalOperator.transactional] with a [Flow] parameter.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
@ExperimentalCoroutinesApi
fun <T : Any> TransactionalOperator.transactional(flow: Flow<T>): Flow<T> =
		transactional(flow.asFlux()).asFlow()

/**
* Coroutines variant of [TransactionalOperator.transactional] with a suspending lambda
* parameter.
*
* @author Sebastien Deleuze
* @since 5.2
*/
suspend fun <T : Any> TransactionalOperator.transactional(f: suspend () -> T?): T? =
		transactional(mono(Dispatchers.Unconfined) { f() }).awaitFirstOrNull()
