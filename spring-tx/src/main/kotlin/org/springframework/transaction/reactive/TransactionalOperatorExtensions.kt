/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.transaction.reactive

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.springframework.transaction.ReactiveTransaction
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Coroutines variant of [TransactionalOperator.transactional] as a [Flow] extension.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
fun <T : Any> Flow<T>.transactional(operator: TransactionalOperator, context: CoroutineContext = EmptyCoroutineContext): Flow<T> =
		operator.transactional(asFlux(context)).asFlow()

/**
* Coroutines variant of [TransactionalOperator.execute] with a suspending lambda
* parameter.
*
* @author Sebastien Deleuze
* @author Mark Paluch
* @since 5.2
*/
suspend fun <T> TransactionalOperator.executeAndAwait(f: suspend (ReactiveTransaction) -> T): T {
	val context = currentCoroutineContext().minusKey(Job.Key)
	@Suppress("UNCHECKED_CAST")
	return execute { status -> mono<T & Any>(context) { f(status) } }.singleOrEmpty().awaitFirstOrNull() as T
}
