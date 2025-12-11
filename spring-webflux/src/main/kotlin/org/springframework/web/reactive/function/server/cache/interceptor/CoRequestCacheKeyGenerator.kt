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

import org.springframework.aop.framework.AopProxyUtils
import org.springframework.aop.support.AopUtils
import org.springframework.cache.interceptor.KeyGenerator
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.context.expression.AnnotatedElementKey
import org.springframework.context.expression.CachedExpressionEvaluator
import org.springframework.context.expression.MethodBasedEvaluationContext
import org.springframework.core.BridgeMethodResolver
import org.springframework.expression.Expression
import org.springframework.web.reactive.function.server.cache.operation.CoRequestCacheOperationSource
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation

/**
 * Key generator for suspend method annotated by
 * [@CoRequestCacheable][org.springframework.web.reactive.function.server.cache.CoRequestCacheable].
 *
 * If the only method parameter is the [Continuation] object, return a [NullaryMethodKey] instance,
 * so that different beans with same method name still have distinct keys.
 *
 * If the method has other parameters, look for the
 * [key expression][org.springframework.web.reactive.function.server.cache.CoRequestCacheable.key]
 * using the [coRequestCacheOperationSource], and return a [SimpleKey] combining the nullary identity with
 * the expression evaluation result, or with all the other parameters for a default blank key.
 *
 * @author Angelo Bracaglia
 * @since 7.0
 */
internal class CoRequestCacheKeyGenerator(
	private val coRequestCacheOperationSource: CoRequestCacheOperationSource,
) : KeyGenerator, CachedExpressionEvaluator() {
	private val bakedExpressions: MutableMap<ExpressionKey, Expression> = ConcurrentHashMap()

	override fun generate(target: Any, method: Method, vararg params: Any?): Any {
		check(params.lastOrNull() is Continuation<*>)

		val targetClass = AopProxyUtils.ultimateTargetClass(target)
		val nullaryMethodKey = NullaryMethodKey(targetClass, method.name)

		if (params.size == 1) {
			return nullaryMethodKey
		}

		val keyExpression: String = coRequestCacheKeyExpression(method, targetClass)

		return if (keyExpression.isBlank()) {
			SimpleKey(nullaryMethodKey, *params.copyOfRange(0, params.size - 1))
		} else {
			val targetMethod = ultimateTargetMethod(method, targetClass)

			val expression =
				getExpression(
					this.bakedExpressions,
					AnnotatedElementKey(targetMethod, targetClass),
					keyExpression
				)

			val context =
				MethodBasedEvaluationContext(
					target,
					targetMethod,
					params,
					parameterNameDiscoverer,
				)

			SimpleKey(nullaryMethodKey, expression.getValue(context))
		}
	}

	private fun coRequestCacheKeyExpression(method: Method, targetClass: Class<*>): String {
		val coRequestCacheOperations = coRequestCacheOperationSource.getCacheOperations(method, targetClass)

		check(1 == coRequestCacheOperations?.size)

		return coRequestCacheOperations.first().key
	}

	private fun ultimateTargetMethod(
		method: Method,
		targetClass: Class<*>
	): Method {
		var method = BridgeMethodResolver.findBridgedMethod(method)

		if (!Proxy.isProxyClass(targetClass)) {
			method = AopUtils.getMostSpecificMethod(method, targetClass)
		}
		return method
	}
}
