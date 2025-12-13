/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.function.server.cache.context

import kotlinx.coroutines.withContext
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

/**
 * Add a [CoRequestCacheContext] element to the web request coroutine context.
 *
 * This [CoWebFilter] is automatically registered when
 * [EnableCoRequestCaching][org.springframework.web.reactive.function.server.cache.EnableCoRequestCaching]
 * is applied to the app configuration.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheWebFilter : CoWebFilter() {
	override suspend fun filter(
		exchange: ServerWebExchange,
		chain: CoWebFilterChain
	) {
		return withContext(CoRequestCacheContext()) {
			chain.filter(exchange)
		}
	}
}
