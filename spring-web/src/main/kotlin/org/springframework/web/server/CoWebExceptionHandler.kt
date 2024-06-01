package org.springframework.web.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

abstract class CoWebExceptionHandler : WebExceptionHandler {
	final override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> =
		mono(Dispatchers.Unconfined) { coHandle(exchange, ex) }.then()


	protected abstract suspend fun coHandle(exchange: ServerWebExchange, ex: Throwable)
}
