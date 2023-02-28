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

package org.springframework.transaction.reactive

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.springframework.transaction.ReactiveTransaction
import java.util.Optional
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
	return execute { status -> mono(context) { f(status) } }.map { value -> Optional.ofNullable(value) }
				.defaultIfEmpty(Optional.empty()).awaitLast().orElse(null)
}
