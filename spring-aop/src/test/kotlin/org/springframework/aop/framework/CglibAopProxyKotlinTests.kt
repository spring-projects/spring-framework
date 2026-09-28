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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

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

	@Test // gh-35487
	fun jvmDefault() {
		val proxyFactory = ProxyFactory()
		proxyFactory.setTarget(AddressRepo())
		proxyFactory.proxy
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClass()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassProceed() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			it.proceed()
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClass()).isEqualTo(ValueClass("foo"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClass()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNull() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			null
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClass()).isNull()
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassNullableValue()).isEqualTo(ValueClassNullableValue("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassNullableValueNull() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue(null)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassNullableValue()).isEqualTo(ValueClassNullableValue(null))
	}

	@Test
	suspend fun proxiedSuspendedInvocationResult() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			Result.success("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnResult().getOrNull()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationString() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			"bar"
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnString()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationAnyAdviceReturnValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnAny()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassPrimitiveValue(1)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(1))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassPrimitiveValueProceed() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			it.proceed()
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(0))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassPrimitiveValue(1)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassPrimitiveValue()).isEqualTo(ValueClassPrimitiveValue(1))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassPrimitiveValueNull() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			null
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassPrimitiveValue()).isNull()
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassNullableValue()).isEqualTo(ValueClassNullableValue("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableValueClassNullableValueNull() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClassNullableValue(null)
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableValueClassNullableValue()).isEqualTo(ValueClassNullableValue(null))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableGenericValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			GenericValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableGenericValueClass()).isEqualTo(GenericValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNestedValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClass(ValueClass("bar"))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNestedValueClass()).isEqualTo(NestedValueClass(ValueClass("bar")))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNestedValueClassPrimitiveValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClassPrimitiveValue(ValueClassPrimitiveValue(1))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNestedValueClassPrimitiveValue())
			.isEqualTo(NestedValueClassPrimitiveValue(ValueClassPrimitiveValue(1)))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNestedValueClassNullableValue() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			NestedValueClassNullableValue(ValueClassNullableValue("bar"))
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNestedValueClassNullableValue())
			.isEqualTo(NestedValueClassNullableValue(ValueClassNullableValue("bar")))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableNonNullGenericValueClass() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			NonNullGenericValueClass("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableNonNullGenericValueClass()).isEqualTo(NonNullGenericValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationNullableResult() {
		val proxyFactory = ProxyFactory(TestBean())
		proxyFactory.addAdvice(MethodInterceptor {
			Result.success("bar")
		})
		val proxy = proxyFactory.proxy as TestBean
		assertThat(proxy.returnNullableResult()?.getOrNull()).isEqualTo("bar")
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverridingAny() {
		val proxyFactory = ProxyFactory(ValueClassOverridingAnyBean())
		proxyFactory.isProxyTargetClass = true
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverridingAnyBean
		assertThat(proxy.returnValue()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverridingGeneric() {
		val proxyFactory = ProxyFactory(ValueClassOverridingGenericBean())
		proxyFactory.isProxyTargetClass = true
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverridingGenericBean
		assertThat(proxy.returnValue()).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverridingGenericParameter() {
		val proxyFactory = ProxyFactory(ValueClassOverridingGenericParameterBean())
		proxyFactory.isProxyTargetClass = true
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverridingGenericParameterBean
		assertThat(proxy.returnValue("foo")).isEqualTo(ValueClass("bar"))
	}

	@Test
	suspend fun proxiedSuspendedInvocationValueClassOverloadingGenericParameter() {
		val proxyFactory = ProxyFactory(ValueClassOverloadingGenericParameterBean())
		proxyFactory.isProxyTargetClass = true
		proxyFactory.addAdvice(MethodInterceptor {
			ValueClass("bar")
		})
		val proxy = proxyFactory.proxy as ValueClassOverloadingGenericParameterBean
		assertThat(proxy.returnValue("foo").value).isEqualTo("bar")
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

	open class AddressRepo(): CrudRepo<Address, Int>

	interface CrudRepo<E : Any, ID : Any> {
		fun save(e: E): E {
			return e
		}
		fun delete(id: ID): Long {
			return 0L
		}
	}

	data class Address(
		val id: Int = 0,
		val street: String,
		val version: Int = 0,
		val createdAt: LocalDateTime? = null,
		val updatedAt: LocalDateTime? = null,
	)

	@Test
	suspend fun proxiedSuspendedInvocationPrivateValueClass() {
		val proxyFactory = ProxyFactory(PrivateValueClassBean())
		proxyFactory.isProxyTargetClass = true
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

	open class TestBean {
		open suspend fun returnValueClass(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}

		open suspend fun returnNullableValueClass(): ValueClass? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnValueClassNullableValue(): ValueClassNullableValue {
			delay(10.milliseconds)
			return ValueClassNullableValue(null)
		}

		open suspend fun returnResult(): Result<String> {
			delay(10.milliseconds)
			return Result.success("foo")
		}

		open suspend fun returnString(): String {
			delay(10.milliseconds)
			return "foo"
		}

		open suspend fun returnAny(): Any {
			delay(10.milliseconds)
			return ValueClass("foo")
		}

		open suspend fun returnValueClassPrimitiveValue(): ValueClassPrimitiveValue {
			delay(10.milliseconds)
			return ValueClassPrimitiveValue(0)
		}

		open suspend fun returnNullableValueClassPrimitiveValue(): ValueClassPrimitiveValue? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNullableValueClassNullableValue(): ValueClassNullableValue? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNullableGenericValueClass(): GenericValueClass<String>? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNestedValueClass(): NestedValueClass {
			delay(10.milliseconds)
			return NestedValueClass(ValueClass("foo"))
		}

		open suspend fun returnNullableNestedValueClassPrimitiveValue(): NestedValueClassPrimitiveValue? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNullableNestedValueClassNullableValue(): NestedValueClassNullableValue? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNullableNonNullGenericValueClass(): NonNullGenericValueClass<String>? {
			delay(10.milliseconds)
			return null
		}

		open suspend fun returnNullableResult(): Result<String>? {
			delay(10.milliseconds)
			return null
		}
	}


	interface AnyBean {
		suspend fun returnValue(): Any
	}

	open class ValueClassOverridingAnyBean : AnyBean {
		override suspend fun returnValue(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}
	}

	interface GenericBean<T> {
		suspend fun returnValue(): T
	}

	open class ValueClassOverridingGenericBean : GenericBean<ValueClass> {
		override suspend fun returnValue(): ValueClass {
			delay(10.milliseconds)
			return ValueClass("foo")
		}
	}

	interface GenericParameterBean<T> {
		suspend fun returnValue(value: T): Any
	}

	open class ValueClassOverridingGenericParameterBean : GenericParameterBean<String> {
		override suspend fun returnValue(value: String): ValueClass {
			delay(10.milliseconds)
			return ValueClass(value)
		}
	}

	open class ValueClassOverloadingGenericParameterBean : GenericParameterBean<Int> {
		override suspend fun returnValue(value: Int): Any {
			delay(10.milliseconds)
			return value
		}

		open suspend fun returnValue(value: String): ValueClass {
			delay(10.milliseconds)
			return ValueClass(value)
		}
	}

	@JvmInline
	private value class PrivateValueClass(val value: String)

	private open class PrivateValueClassBean {
		open suspend fun returnValue(): PrivateValueClass {
			delay(10.milliseconds)
			return PrivateValueClass("foo")
		}
	}

}
