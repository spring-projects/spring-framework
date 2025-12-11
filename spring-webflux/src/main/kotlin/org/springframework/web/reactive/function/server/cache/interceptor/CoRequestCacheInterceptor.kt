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

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.apache.commons.logging.LogFactory
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.web.reactive.function.server.cache.context.CoRequestCacheContext
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation
import kotlin.reflect.jvm.jvmName

private val logger = LogFactory.getLog(CoRequestCacheInterceptor::class.java)

/**
 * AOP Alliance MethodInterceptor for request-scoped caching of Kotlin suspend method invocations.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheInterceptor(private val keyGenerator: KeyGenerator) : MethodInterceptor {

	/**
	 * Use the provided [keyGenerator] to generate a unique key for the intercepted suspend method call.
	 *
	 * When not already present for the generated key, create and store in the [CoRequestCacheContext] element of the
	 * web request coroutine a lazy cached version of the [invocation] result:
	 *
	 * - A [shared Mono][Mono.share], for [Mono] type.
	 * - A [buffered][Flux.buffer], [flattened][Flux.flatMapIterable], [replayed Flux][Flux.replay], for [Flux] type.
	 *
	 * The suspend method result is expected to be already converted to a reactive type by the
	 * [AopUtils.invokeJoinpointUsingReflection][org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection]
	 * AOP utility.
	 *
	 * @author Angelo Bracaglia
	 * @since 7.0
	 */
	override fun invoke(invocation: MethodInvocation): Any? {
		val coRequestCache =
			(invocation.arguments.lastOrNull() as? Continuation<*>)
				?.context[CoRequestCacheContext.Key]?.cache
				?: run {
					if (logger.isWarnEnabled) {
						logger.warn(
							"Skip CoRequestCaching for ${invocation.method}: coroutine cache context not available"
						)
					}
					return invocation.proceed()
				}

		val targetObject = checkNotNull(invocation.getThis())

		val coRequestCacheKey = keyGenerator.generate(targetObject, invocation.method, *invocation.arguments)

		return coRequestCache.computeIfAbsent(coRequestCacheKey) {
			when (val publisher = invocation.proceed()) {
				is Mono<*> -> publisher.share()
				is Flux<*> ->
					publisher
						.buffer()
						.flatMapIterable { it }
						.replay()
						.refCount(1)

				else -> throw IllegalArgumentException("Unexpected type ${publisher?.let { it::class.jvmName }}")
			}
		}
	}
}
