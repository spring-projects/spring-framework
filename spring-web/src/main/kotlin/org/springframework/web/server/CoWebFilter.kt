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

package org.springframework.web.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

/**
 * Kotlin-specific implementation of the [WebFilter] interface that allows for
 * using coroutines.
 *
 * @author Arjen Poutsma
 * @since 6.0.5
 */
abstract class CoWebFilter : WebFilter {

	final override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		return mono(Dispatchers.Unconfined) {
			filter(exchange, object : CoWebFilterChain {
				override suspend fun filter(exchange: ServerWebExchange) {
					chain.filter(exchange).awaitSingleOrNull()
				}
			})}.then()
	}

	/**
	 * Process the Web request and (optionally) delegate to the next
	 * [WebFilter] through the given [WebFilterChain].
	 * @param exchange the current server exchange
	 * @param chain provides a way to delegate to the next filter
	 */
	protected abstract suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain)

}

/**
 * Kotlin-specific adaption of [WebFilterChain] that allows for coroutines.
 *
 * @author Arjen Poutsma
 * @since 6.0.5
 */
interface CoWebFilterChain {

	/**
	 * Delegate to the next [WebFilter] in the chain.
	 * @param exchange the current server exchange
	 */
	suspend fun filter(exchange: ServerWebExchange)

}