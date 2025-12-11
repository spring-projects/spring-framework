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

package org.springframework.web.reactive.function.server.cache.interceptor

import org.aopalliance.aop.Advice
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheOperationSource
import java.lang.reflect.Method

/**
 * Advisor driven by a [coRequestCacheOperationSource], used to match suspend methods that are cacheable for the lifespan
 * of the coroutine handling a web request.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheAdvisor(
	val coRequestCacheOperationSource: CoRequestCacheOperationSource,
	coRequestCacheAdvice: Advice,
) : StaticMethodMatcherPointcutAdvisor(coRequestCacheAdvice) {
	override fun matches(method: Method, targetClass: Class<*>): Boolean =
		coRequestCacheOperationSource.hasCacheOperations(method, targetClass)
}
