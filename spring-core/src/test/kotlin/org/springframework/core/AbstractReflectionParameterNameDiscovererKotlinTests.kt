/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.springframework.util.ReflectionUtils
import kotlin.coroutines.Continuation

/**
 * Abstract tests for Kotlin [ParameterNameDiscoverer] aware implementations.
 *
 * @author Sebastien Deleuze
 */
@Suppress("UNUSED_PARAMETER")
abstract class AbstractReflectionParameterNameDiscovererKotlinTests(protected val parameterNameDiscoverer: ParameterNameDiscoverer) {

	@Test
	fun getParameterNamesOnInterface() {
		val method = ReflectionUtils.findMethod(MessageService::class.java, "sendMessage", String::class.java)!!
		val actualParams = parameterNameDiscoverer.getParameterNames(method)
		assertThat(actualParams).containsExactly("message")
	}

	@Test
	fun getParameterNamesOnClass() {
		val constructor = ReflectionUtils.accessibleConstructor(MessageServiceImpl::class.java,String::class.java)
		val actualConstructorParams = parameterNameDiscoverer.getParameterNames(constructor)
		assertThat(actualConstructorParams).containsExactly("message")
		val method = ReflectionUtils.findMethod(MessageServiceImpl::class.java, "sendMessage", String::class.java)!!
		val actualMethodParams = parameterNameDiscoverer.getParameterNames(method)
		assertThat(actualMethodParams).containsExactly("message")
	}

	@Test
	fun getParameterNamesOnSuspendingFunction() {
		val method = ReflectionUtils.findMethod(CoroutinesMessageService::class.java, "sendMessage",
			String::class.java, Continuation::class.java)!!
		val actualMethodParams = parameterNameDiscoverer.getParameterNames(method)
		assertThat(actualMethodParams).containsExactly("message")
	}

	@Test
	fun getParameterNamesOnExtensionMethod() {
		val method = ReflectionUtils.findMethod(UtilityClass::class.java, "identity", String::class.java)!!
		val actualParams = parameterNameDiscoverer.getParameterNames(method)!!
		assertThat(actualParams).containsExactly("\$receiver")
	}

	interface MessageService {
		fun sendMessage(message: String)
	}

	class MessageServiceImpl(message: String) {
		fun sendMessage(message: String) = message
	}

	class UtilityClass {
		fun String.identity() = this
	}

	class CoroutinesMessageService {
		suspend fun sendMessage(message: String) = message
	}

}
