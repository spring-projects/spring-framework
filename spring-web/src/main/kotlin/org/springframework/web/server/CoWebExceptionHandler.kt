/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext

/**
 * Kotlin-specific implementation of the [WebExceptionHandler] interface that allows for
 * using coroutines, including [kotlin.coroutines.CoroutineContext] propagation.
 *
 * @author Sangyoon Jeong
 * @since 6.2
 */
abstract class CoWebExceptionHandler : WebExceptionHandler {

	final override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
		val context = exchange.attributes[CoWebFilter.COROUTINE_CONTEXT_ATTRIBUTE] as CoroutineContext?
		return mono(context ?: Dispatchers.Unconfined) { coHandle(exchange, ex) }.then()
	}

	protected abstract suspend fun coHandle(exchange: ServerWebExchange, ex: Throwable)
}
