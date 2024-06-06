package org.springframework.web.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext

abstract class CoWebExceptionHandler : WebExceptionHandler {
	final override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
		val context = exchange.attributes[CoWebFilter.COROUTINE_CONTEXT_ATTRIBUTE] as CoroutineContext?
		return mono(context ?: Dispatchers.Unconfined) { coHandle(exchange, ex) }.then()
	}

	protected abstract suspend fun coHandle(exchange: ServerWebExchange, ex: Throwable)
}
