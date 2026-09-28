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
 * @author Sebastien Deleuze
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

	@Test
	suspend fun proxiedSuspendedInvocationValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassPrimitiveValue(1)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(1))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassPrimitiveValueProceed() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			it.proceed()
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(0))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassPrimitiveValue(1)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(1))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassPrimitiveValueNull() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			null
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassPrimitiveValue()).isNull()
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassNullableValue()).isEqualTo(ValueClassNullableValue("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNullableValueNull() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue(null)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassNullableValue()).isEqualTo(ValueClassNullableValue(null))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableGenericValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			GenericValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableGenericValueClass()).isEqualTo(GenericValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNestedValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClass(ValueClass("bar"))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNestedValueClass()).isEqualTo(NestedValueClass(ValueClass("bar")))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNestedValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClassPrimitiveValue(ValueClassPrimitiveValue(1))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNestedValueClassPrimitiveValue())
			.isEqualTo(NestedValueClassPrimitiveValue(ValueClassPrimitiveValue(1)))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNestedValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClassNullableValue(ValueClassNullableValue("bar"))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNestedValueClassNullableValue())
			.isEqualTo(NestedValueClassNullableValue(ValueClassNullableValue("bar")))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNonNullGenericValueClass() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			NonNullGenericValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNonNullGenericValueClass()).isEqualTo(NonNullGenericValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableResult() {
		val proxyFactory = ProxyFactory(TestBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			Result.success("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableResult()?.getOrNull()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverridingAny() {
		val proxyFactory = ProxyFactory(ValueClassOverridingAnyBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverridingAnyBean
		assertThat(proxy.returnValue()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverridingGeneric() {
		val proxyFactory = ProxyFactory(ValueClassOverridingGenericBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverridingGenericBean
		assertThat(proxy.returnValue()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationPrivateValueClass() {
		val proxyFactory = ProxyFactory(PrivateValueClassBeanImpl())
		proxyFactory.addAdvice(MethodInterceptor {
			PrivateValueClass("bar")
		})
		val proxy = proxyFactory.proxy as PrivateValueClassBean
		assertThat(proxy.returnValue()).isEqualTo(PrivateValueClass("bar"))
	}

	@JvmInline
	value class ValueClass(val value: String)

	@JvmInline
	value class ValueClassNullableValue(val value: String?)

	@JvmInline
	value class ValueClassPrimitiveValue(val value: Int)

	@JvmInline
	value class GenericValueClass<T>(val value: T)

	@JvmInline
	value class NestedValueClass(val value: ValueClass)

	@JvmInline
	value class NestedValueClassPrimitiveValue(val value: ValueClassPrimitiveValue)

	@JvmInline
	value class NestedValueClassNullableValue(val value: ValueClassNullableValue)

	@JvmInline
	value class NonNullGenericValueClass<T : Any>(val value: T)

	interface TestBean {
		suspend fun returnValueClass(): ValueClass

		suspend fun returnNullableValueClass(): ValueClass?

		suspend fun returnValueClassNullableValue(): ValueClassNullableValue

		suspend fun returnResult(): Result<String>

		suspend fun returnString(): String

		suspend fun returnAny(): Any

		suspend fun returnValueClassPrimitiveValue(): ValueClassPrimitiveValue

		suspend fun returnNullableValueClassPrimitiveValue(): ValueClassPrimitiveValue?

		suspend fun returnNullableValueClassNullableValue(): ValueClassNullableValue?

		suspend fun returnNullableGenericValueClass(): GenericValueClass<String>?

		suspend fun returnNestedValueClass(): NestedValueClass

		suspend fun returnNullableNestedValueClassPrimitiveValue(): NestedValueClassPrimitiveValue?

		suspend fun returnNullableNestedValueClassNullableValue(): NestedValueClassNullableValue?

		suspend fun returnNullableNonNullGenericValueClass(): NonNullGenericValueClass<String>?

		suspend fun returnNullableResult(): Result<String>?
	}

	class TestBeanImpl : TestBean {
		override suspend fun returnValueClass(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}

		override suspend fun returnNullableValueClass(): ValueClass? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnValueClassNullableValue(): ValueClassNullableValue {
			delay(10.milliseconds)
			return ValueClassNullableValue(null)
		}

		override suspend fun returnResult(): Result<String> {
			delay(10.milliseconds)
			return Result.success("foo")
		}

		override suspend fun returnString(): String {
			delay(10.milliseconds)
			return "foo"
		}

		override suspend fun returnAny(): Any {
			delay(10.milliseconds)
			return ValueClass("foo")
		}

		override suspend fun returnValueClassPrimitiveValue(): ValueClassPrimitiveValue {
			delay(10.milliseconds)
			return ValueClassPrimitiveValue(0)
		}

		override suspend fun returnNullableValueClassPrimitiveValue(): ValueClassPrimitiveValue? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNullableValueClassNullableValue(): ValueClassNullableValue? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNullableGenericValueClass(): GenericValueClass<String>? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNestedValueClass(): NestedValueClass {
			delay(10.milliseconds)
			return NestedValueClass(ValueClass("foo"))
		}

		override suspend fun returnNullableNestedValueClassPrimitiveValue(): NestedValueClassPrimitiveValue? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNullableNestedValueClassNullableValue(): NestedValueClassNullableValue? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNullableNonNullGenericValueClass(): NonNullGenericValueClass<String>? {
			delay(10.milliseconds)
			return null
		}

		override suspend fun returnNullableResult(): Result<String>? {
			delay(10.milliseconds)
			return null
		}
	}


	interface AnyBean {
		suspend fun returnValue(): Any
	}

	interface ValueClassOverridingAnyBean : AnyBean {
		override suspend fun returnValue(): ValueClass
	}

	class ValueClassOverridingAnyBeanImpl : ValueClassOverridingAnyBean {
		override suspend fun returnValue(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}
	}

	interface GenericBean<T> {
		suspend fun returnValue(): T
	}

	interface ValueClassOverridingGenericBean : GenericBean<ValueClass> {
		override suspend fun returnValue(): ValueClass
	}

	class ValueClassOverridingGenericBeanImpl : ValueClassOverridingGenericBean {
		override suspend fun returnValue(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}
	}

	@JvmInline
	private value class PrivateValueClass(val value: String)

	private interface PrivateValueClassBean {
		suspend fun returnValue(): PrivateValueClass
	}

	private class PrivateValueClassBeanImpl : PrivateValueClassBean {
		override suspend fun returnValue(): PrivateValueClass {
			delay(10.milliseconds)
			return PrivateValueClass("foo")
		}
	}

}
