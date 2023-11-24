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

package org.springframework.web.reactive.function.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono

/**
 * Kotlin-specific implementation of the [ExchangeFilterFunction] interface
 * that allows for using coroutines.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
abstract class CoExchangeFilterFunction : ExchangeFilterFunction {

	final override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
		return mono(Dispatchers.Unconfined) {
			filter(request, object : CoExchangeFunction {
				override suspend fun exchange(request: ClientRequest): ClientResponse {
					return next.exchange(request).awaitSingle()
				}
			})
		}
	}

	/**
	 * Apply this filter to the given request and exchange function.
	 *
	 * The given [CoExchangeFunction] represents the next entity in the
	 * chain, to be invoked via [CoExchangeFunction.exchange] in order to
	 * proceed with the exchange, or not invoked to short-circuit the chain.
	 *
	 * **Note:** When a filter handles the response after the
	 * call to [CoExchangeFunction.exchange], extra care must be taken to
	 * always consume its content or otherwise propagate it downstream for
	 * further handling, for example by the [WebClient]. Please see the
	 * reference documentation for more details on this.
	 *
	 * @param request the current request
	 * @param next the next exchange function in the chain
	 * @return the filtered response
	 */
	protected abstract suspend fun filter(request: ClientRequest, next: CoExchangeFunction): ClientResponse
}


/**
 * Kotlin-specific adaption of [ExchangeFunction] that allows for coroutines.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
interface CoExchangeFunction {

	/**
	 * Exchange the given request for a [ClientResponse].
	 *
	 * **Note:** When calling this method from an
	 * [CoExchangeFilterFunction] that handles the response in some way,
	 * extra care must be taken to always consume its content or otherwise
	 * propagate it downstream for further handling, for example by the
	 * [WebClient]. Please see the reference documentation for more
	 * details on this.
	 *
	 * @param request the request to exchange
	 * @return the delayed response
	 */
	suspend fun exchange(request: ClientRequest): ClientResponse
}
