/*
 * Copyright 2002-2023 the original author or authors.
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
import java.lang.reflect.Method
import java.lang.reflect.TypeVariable
import kotlin.coroutines.Continuation
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod

/**
 * Tests for Kotlin support in [MethodParameter].
 *
 * @author Raman Gupta
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Konrad Kaminski
 */
class MethodParameterKotlinTests {

	private val nullableMethod: Method = javaClass.getMethod("nullable", String::class.java)

	private val nonNullableMethod = javaClass.getMethod("nonNullable", String::class.java)

	private val withDefaultValueMethod: Method = javaClass.getMethod("withDefaultValue", String::class.java)

	private val innerClassConstructor = InnerClass::class.java.getConstructor(MethodParameterKotlinTests::class.java)

	private val innerClassWithParametersConstructor = InnerClassWithParameter::class.java
			.getConstructor(MethodParameterKotlinTests::class.java, String::class.java, String::class.java)

	private val regularClassConstructor = RegularClass::class.java.getConstructor(String::class.java, String::class.java)


	@Test
	fun `Method parameter nullability`() {
		assertThat(MethodParameter(nullableMethod, 0).isOptional).isTrue()
		assertThat(MethodParameter(nonNullableMethod, 0).isOptional).isFalse()
	}

	@Test
	fun `Method parameter with default value`() {
		assertThat(MethodParameter(withDefaultValueMethod, 0).isOptional).isTrue()
	}

	@Test
	fun `Method parameter without default value`() {
		assertThat(MethodParameter(nonNullableMethod, 0).isOptional).isFalse()
	}

	@Test
	fun `Method return type nullability`() {
		assertThat(MethodParameter(nullableMethod, -1).isOptional).isTrue()
		assertThat(MethodParameter(nonNullableMethod, -1).isOptional).isFalse()
	}

	@Test  // SPR-17222
	fun `Inner class constructor`() {
		assertThat(MethodParameter(innerClassConstructor, 0).isOptional).isFalse()
		assertThat(MethodParameter(innerClassWithParametersConstructor, 0).isOptional).isFalse()
		assertThat(MethodParameter(innerClassWithParametersConstructor, 1).isOptional).isFalse()
		assertThat(MethodParameter(innerClassWithParametersConstructor, 2).isOptional).isTrue()
	}

	@Test
	fun `Regular class constructor`() {
		assertThat(MethodParameter(regularClassConstructor, 0).isOptional).isFalse()
		assertThat(MethodParameter(regularClassConstructor, 1).isOptional).isTrue()
	}

	@Test
	fun `Suspending function return type`() {
		assertThat(returnParameterType("suspendFun")).isEqualTo(Number::class.java)
		assertThat(returnGenericParameterType("suspendFun")).isEqualTo(Number::class.java)

		assertThat(returnParameterType("suspendFun2")).isEqualTo(Producer::class.java)
		assertThat(returnGenericParameterTypeName("suspendFun2")).isEqualTo("org.springframework.core.Producer<? extends java.lang.Number>")

		assertThat(returnParameterType("suspendFun3")).isEqualTo(Wrapper::class.java)
		assertThat(returnGenericParameterTypeName("suspendFun3")).isEqualTo("org.springframework.core.Wrapper<java.lang.Number>")

		assertThat(returnParameterType("suspendFun4")).isEqualTo(Consumer::class.java)
		assertThat(returnGenericParameterTypeName("suspendFun4")).isEqualTo("org.springframework.core.Consumer<? super java.lang.Number>")

		assertThat(returnParameterType("suspendFun5")).isEqualTo(Producer::class.java)
		assertThat(returnGenericParameterType("suspendFun5")).isInstanceOf(TypeVariable::class.java)
		assertThat(returnGenericParameterTypeBoundName("suspendFun5")).isEqualTo("org.springframework.core.Producer<? extends java.lang.Number>")

		assertThat(returnParameterType("suspendFun6")).isEqualTo(Wrapper::class.java)
		assertThat(returnGenericParameterType("suspendFun6")).isInstanceOf(TypeVariable::class.java)
		assertThat(returnGenericParameterTypeBoundName("suspendFun6")).isEqualTo("org.springframework.core.Wrapper<java.lang.Number>")

		assertThat(returnParameterType("suspendFun7")).isEqualTo(Consumer::class.java)
		assertThat(returnGenericParameterType("suspendFun7")).isInstanceOf(TypeVariable::class.java)
		assertThat(returnGenericParameterTypeBoundName("suspendFun7")).isEqualTo("org.springframework.core.Consumer<? super java.lang.Number>")

		assertThat(returnParameterType("suspendFun8")).isEqualTo(Object::class.java)
		assertThat(returnGenericParameterType("suspendFun8")).isEqualTo(Object::class.java)
	}

	@Test
	fun `Parameter name for regular function`() {
		val methodParameter = returnMethodParameter("nullable", 0)
		methodParameter.initParameterNameDiscovery(KotlinReflectionParameterNameDiscoverer())
		assertThat(methodParameter.getParameterName()).isEqualTo("nullable")
	}

	@Test
	fun `Parameter name for suspending function`() {
		val methodParameter = returnMethodParameter("suspendFun", 0)
		methodParameter.initParameterNameDiscovery(KotlinReflectionParameterNameDiscoverer())
		assertThat(methodParameter.getParameterName()).isEqualTo("p1")
	}

	@Test
	fun `Continuation parameter name for suspending function`() {
		val methodParameter = returnMethodParameter("suspendFun", 1)
		methodParameter.initParameterNameDiscovery(KotlinReflectionParameterNameDiscoverer())
		assertThat(methodParameter.getParameterName()).isNull()
	}

	@Test
	fun `Continuation parameter is optional`() {
		val method = this::class.java.getDeclaredMethod("suspendFun", String::class.java, Continuation::class.java)
		assertThat(MethodParameter(method, 0).isOptional).isFalse()
		assertThat(MethodParameter(method, 1).isOptional).isTrue()
	}

	private fun returnParameterType(funName: String) = returnMethodParameter(funName).parameterType
	private fun returnGenericParameterType(funName: String) = returnMethodParameter(funName).genericParameterType
	private fun returnGenericParameterTypeName(funName: String) = returnGenericParameterType(funName).typeName
	private fun returnGenericParameterTypeBoundName(funName: String) = (returnGenericParameterType(funName) as TypeVariable<*>).bounds[0].typeName

	private fun returnMethodParameter(funName: String, parameterIndex: Int = -1) =
		MethodParameter(this::class.declaredFunctions.first { it.name == funName }.javaMethod!!, parameterIndex)

	@Suppress("unused_parameter")
	fun nullable(nullable: String?): Int? = 42

	@Suppress("unused_parameter")
	fun nonNullable(nonNullable: String): Int = 42

	fun withDefaultValue(withDefaultValue: String = "default") = withDefaultValue

	inner class InnerClass

	@Suppress("unused_parameter")
	inner class InnerClassWithParameter(nonNullable: String, nullable: String?)

	@Suppress("unused_parameter")
	class RegularClass(nonNullable: String, nullable: String?)

	@Suppress("unused", "unused_parameter")
	suspend fun suspendFun(p1: String): Number = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun suspendFun2(p1: String): Producer<Number> = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun suspendFun3(p1: String): Wrapper<Number> = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun suspendFun4(p1: String): Consumer<Number> = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun <T: Producer<Number>> suspendFun5(p1: String): T = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun <T: Wrapper<Number>> suspendFun6(p1: String): T = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun <T: Consumer<Number>> suspendFun7(p1: String): T = TODO()

	@Suppress("unused", "unused_parameter")
	suspend fun suspendFun8(p1: String): Any? = TODO()
}

interface Producer<out T>

interface Wrapper<T>

interface Consumer<in T>
