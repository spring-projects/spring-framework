package org.springframework.transaction.reactive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.springframework.transaction.ReactiveTransaction

/**
 * Coroutines variant of [TransactionalOperator.transactional] as a [Flow] extension.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun <T : Any> Flow<T>.transactional(operator: TransactionalOperator): Flow<T> =
		operator.transactional(asFlux()).asFlow()

/**
* Coroutines variant of [TransactionalOperator.execute] with a suspending lambda
* parameter.
*
* @author Sebastien Deleuze
* @since 5.2
*/
suspend fun <T : Any> TransactionalOperator.executeAndAwait(f: suspend (ReactiveTransaction) -> T?): T? =
		execute { status -> mono(Dispatchers.Unconfined) { f(status) } }.awaitFirstOrNull()
