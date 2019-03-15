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

package org.springframework.web.reactive.result.method

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.onErrorMap
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

/**
 * Invoke an handler method converting suspending method to {@link Mono} if necessary.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
internal fun invokeHandlerMethod(method: Method, bean: Any, vararg args: Any?): Any? {
	val function = method.kotlinFunction!!
	return if (function.isSuspend) {
		GlobalScope.mono { function.callSuspend(bean, *args.sliceArray(0..(args.size-2)))
				.let { if (it == Unit) null else it} }
				.onErrorMap(InvocationTargetException::class) { it.targetException }
	}
	else {
		function.call(bean, *args)
	}
}