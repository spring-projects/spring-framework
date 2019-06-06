/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.springframework.util.ReflectionUtils

/**
 * Tests for KotlinReflectionParameterNameDiscoverer
 */
class KotlinReflectionParameterNameDiscovererTests {

	private val parameterNameDiscoverer = KotlinReflectionParameterNameDiscoverer()

	@Test
	fun getParameterNamesOnInterface() {
		val method = ReflectionUtils.findMethod(MessageService::class.java,"sendMessage", String::class.java)!!
		val actualParams = parameterNameDiscoverer.getParameterNames(method)
		assertThat(actualParams, `is`(arrayOf("message")))
	}

	@Test
	fun getParameterNamesOnClass() {
		val method = ReflectionUtils.findMethod(MessageServiceImpl::class.java,"sendMessage", String::class.java)!!
		val actualParams = parameterNameDiscoverer.getParameterNames(method)
		assertThat(actualParams, `is`(arrayOf("message")))
	}

	@Test
	fun getParameterNamesOnExtensionMethod() {
		val method = ReflectionUtils.findMethod(UtilityClass::class.java, "identity", String::class.java)!!
		val actualParams = parameterNameDiscoverer.getParameterNames(method)!!
		assertThat(actualParams, `is`(arrayOf("\$receiver")))
	}

	interface MessageService {
		fun sendMessage(message: String)
	}

	class MessageServiceImpl {
		fun sendMessage(message: String) = message
	}

	class UtilityClass {
		fun String.identity() = this
	}
}
