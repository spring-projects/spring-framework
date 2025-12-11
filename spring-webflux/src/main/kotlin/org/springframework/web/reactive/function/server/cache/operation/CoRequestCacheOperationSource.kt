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

package org.springframework.web.reactive.function.server.cache.operation

import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource
import org.springframework.cache.interceptor.CacheOperation
import org.springframework.core.KotlinDetector
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.reactive.function.server.cache.CoRequestCacheable
import java.lang.reflect.Method

/**
 * Implementation of [CacheOperationSource][org.springframework.cache.interceptor.CacheOperationSource]
 * interface detecting [CoRequestCacheable] annotations on suspend methods and exposing the corresponding
 * [CoRequestCacheableOperation].
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheOperationSource : AbstractFallbackCacheOperationSource() {
	override fun findCacheOperations(type: Class<*>): Collection<CacheOperation>? = null

	override fun findCacheOperations(method: Method): Collection<CacheOperation>? {
		if (!KotlinDetector.isSuspendingFunction(method)) return null

		val coRequestCacheable =
			AnnotatedElementUtils
				.findMergedAnnotation(method, CoRequestCacheable::class.java) ?: return null

		val coRequestCacheableOperation =
			CoRequestCacheableOperation
				.Builder()
				.apply { key = coRequestCacheable.key }
				.build()

		return listOf(coRequestCacheableOperation)
	}
}
