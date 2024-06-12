/*
 * Copyright 2002-2024 the original author or authors.
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
