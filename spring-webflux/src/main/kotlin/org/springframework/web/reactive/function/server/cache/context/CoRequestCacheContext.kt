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

import org.reactivestreams.Publisher
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * A coroutine context element holding the [cache] values for
 * [@CoRequestCacheable][org.springframework.web.reactive.function.server.cache.CoRequestCacheable]
 * annotated methods.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheContext(
	val cache: ConcurrentHashMap<Any, Publisher<*>> = ConcurrentHashMap()
) : AbstractCoroutineContextElement(Key) {
	companion object Key : CoroutineContext.Key<CoRequestCacheContext>
}
