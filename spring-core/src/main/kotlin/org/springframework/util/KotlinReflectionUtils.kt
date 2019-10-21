/*
 * Copyright 2002-2019 the original author or authors.
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

@file:JvmName("KotlinReflectionUtils")
package org.springframework.util

import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

/**
 * Convert a [Method] to a [KFunction].
 *
 * @author Michael Gmeiner
 * @since 5.2
 */
internal fun methodToFunction(method: Method) = method.kotlinFunction
		?: throw IllegalStateException("Could not convert Java method to Kotlin function")

/**
 * Make the given [KFunction] accessible if necessary.
 * The [isAccessible] setter is only called when actually necessary, to avoid unnecessary conflicts with a JVM
 * SecurityManager (if active).
 *
 * @author Michael Gmeiner
 * @since 5.2
 */
internal fun makeFunctionAccessible(function: KFunction<*>) {
	if (!function.isAccessible) {
		function.isAccessible = true
	}
}
