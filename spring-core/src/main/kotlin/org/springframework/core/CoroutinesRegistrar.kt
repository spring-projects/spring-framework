/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.toMono

/**
 * Register Reactive adapters for Coroutines types.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
internal fun registerAdapter(registry: ReactiveAdapterRegistry) {
	registry.registerReactiveType(
			ReactiveTypeDescriptor.singleOptionalValue(Deferred::class.java) { GlobalScope.async {} },
			{ source -> GlobalScope.mono { (source as Deferred<*>).await() }},
			{ source -> GlobalScope.async { source.toMono().awaitFirstOrNull() } }
	)
}
