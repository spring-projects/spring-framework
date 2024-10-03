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

package org.springframework.aop.framework

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

/**
 * Tests for Kotlin support in [CglibAopProxy].
 *
 * @author Sebastien Deleuze
 */
class CglibAopProxyKotlinTests {

	@Test
	fun proxiedInvocation() {
		val proxyFactory = ProxyFactory(MyKotlinBean())
		val proxy = proxyFactory.proxy as MyKotlinBean
		assertThat(proxy.capitalize("foo")).isEqualTo("FOO")
	}

	@Test
	fun proxiedUncheckedException() {
		val proxyFactory = ProxyFactory(MyKotlinBean())
		val proxy = proxyFactory.proxy as MyKotlinBean
		assertThatThrownBy { proxy.uncheckedException() }.isInstanceOf(IllegalStateException::class.java)
	}

	@Test
	fun proxiedCheckedException() {
		val proxyFactory = ProxyFactory(MyKotlinBean())
		val proxy = proxyFactory.proxy as MyKotlinBean
		assertThatThrownBy { proxy.checkedException() }.isInstanceOf(CheckedException::class.java)
	}


	open class MyKotlinBean {

		open fun capitalize(value: String) = value.uppercase()

		open fun uncheckedException() {
			throw IllegalStateException()
		}

		open fun checkedException() {
			throw CheckedException()
		}
	}

	class CheckedException() : Exception()
}
