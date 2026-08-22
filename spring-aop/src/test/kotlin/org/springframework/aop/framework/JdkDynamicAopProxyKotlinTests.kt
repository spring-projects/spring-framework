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

package org.springframework.aop.framework

import kotlinx.coroutines.delay
import org.aopalliance.intercept.MethodInterceptor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tests for Kotlin support in [JdkDynamicAopProxy].
 *
 * @author Dmitry Sulman
 */
class JdkDynamicAopProxyKotlinTests {

	@Test
	suspend fun proxiedSuspendedInvocationValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClass()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassProceed() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			it.proceed()
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClass()).isEqualTo(ValueClass("foo"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClass()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNull() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			null
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClass()).isNull()
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassNullableValue()).isEqualTo(ValueClassNullableValue("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassNullableValueNull() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue(null)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassNullableValue()).isEqualTo(ValueClassNullableValue(null))
	}

	@Test
	suspend fun proxiedSuspendedInvocationResult() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			Result.success("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnResult().getOrNull()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationString() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			"bar"
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnString()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationAnyAdviceReturnValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnAny()).isEqualTo(ValueClass("bar"))
	}

	@JvmInline
	value class ValueClass(val value: String)

	@JvmInline
	value class ValueClassNullableValue(val value: String?)

	interface TestBean {
		suspend fun returnValueClass(): ValueClass

		suspend fun returnNullableValueClass(): ValueClass?

		suspend fun returnValueClassNullableValue(): ValueClassNullableValue

		suspend fun returnResult(): Result<String>

		suspend fun returnString(): String

		suspend fun returnAny(): Any
	}

	class TestBeanImpl : TestBean {
		override suspend fun returnValueClass(): ValueClass {
			delay(1000.milliseconds)
			return ValueClass("foo")
		}

		override suspend fun returnNullableValueClass(): ValueClass? {
			delay(1000.milliseconds)
			return null
		}

		override suspend fun returnValueClassNullableValue(): ValueClassNullableValue {
			delay(1000.milliseconds)
			return ValueClassNullableValue(null)
		}

		override suspend fun returnResult(): Result<String> {
			delay(1000.milliseconds)
			return Result.success("foo")
		}

		override suspend fun returnString(): String {
			delay(1000.milliseconds)
			return "foo"
		}

		override suspend fun returnAny(): Any {
			delay(1000.milliseconds)
			return ValueClass("foo")
		}
	}

}