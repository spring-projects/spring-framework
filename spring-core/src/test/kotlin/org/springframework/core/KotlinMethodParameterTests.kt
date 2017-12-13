/*
 * Copyright 2002-2018 the original author or authors.
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
package org.springframework.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method
import java.lang.reflect.TypeVariable
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
class KotlinMethodParameterTests {

	private val nullableMethod: Method = javaClass.getMethod("nullable", String::class.java)

	private val nonNullableMethod = javaClass.getMethod("nonNullable", String::class.java)

	private val innerClassConstructor = InnerClass::class.java.getConstructor(KotlinMethodParameterTests::class.java)

	private val innerClassWithParametersConstructor = InnerClassWithParameter::class.java
			.getConstructor(KotlinMethodParameterTests::class.java, String::class.java, String::class.java)

	private val regularClassConstructor = RegularClass::class.java.getConstructor(String::class.java, String::class.java)


	@Test
	fun `Method parameter nullability`() {
		assertTrue(MethodParameter(nullableMethod, 0).isOptional)
		assertFalse(MethodParameter(nonNullableMethod, 0).isOptional)
	}

	@Test
	fun `Method return type nullability`() {
		assertTrue(MethodParameter(nullableMethod, -1).isOptional)
		assertFalse(MethodParameter(nonNullableMethod, -1).isOptional)
	}

	@Test  // SPR-17222
	fun `Inner class constructor`() {
		assertFalse(MethodParameter(innerClassConstructor, 0).isOptional)

		assertFalse(MethodParameter(innerClassWithParametersConstructor, 0).isOptional)
		assertFalse(MethodParameter(innerClassWithParametersConstructor, 1).isOptional)
		assertTrue(MethodParameter(innerClassWithParametersConstructor, 2).isOptional)
	}

	@Test
	fun `Regular class constructor`() {
		assertFalse(MethodParameter(regularClassConstructor, 0).isOptional)
		assertTrue(MethodParameter(regularClassConstructor, 1).isOptional)
	}

	@Test
	fun `Suspending function return type`() {
		assertEquals(Number::class.java, returnParameterType("suspendFun"))
		assertEquals(Number::class.java, returnGenericParameterType("suspendFun"))

		assertEquals(Producer::class.java, returnParameterType("suspendFun2"))
		assertEquals("org.springframework.core.Producer<? extends java.lang.Number>", returnGenericParameterTypeName("suspendFun2"))

		assertEquals(Wrapper::class.java, returnParameterType("suspendFun3"))
		assertEquals("org.springframework.core.Wrapper<java.lang.Number>", returnGenericParameterTypeName("suspendFun3"))

		assertEquals(Consumer::class.java, returnParameterType("suspendFun4"))
		assertEquals("org.springframework.core.Consumer<? super java.lang.Number>", returnGenericParameterTypeName("suspendFun4"))

		assertEquals(Producer::class.java, returnParameterType("suspendFun5"))
		assertTrue(returnGenericParameterType("suspendFun5") is TypeVariable<*>)
		assertEquals("org.springframework.core.Producer<? extends java.lang.Number>", returnGenericParameterTypeBoundName("suspendFun5"))

		assertEquals(Wrapper::class.java, returnParameterType("suspendFun6"))
		assertTrue(returnGenericParameterType("suspendFun6") is TypeVariable<*>)
		assertEquals("org.springframework.core.Wrapper<java.lang.Number>", returnGenericParameterTypeBoundName("suspendFun6"))

		assertEquals(Consumer::class.java, returnParameterType("suspendFun7"))
		assertTrue(returnGenericParameterType("suspendFun7") is TypeVariable<*>)
		assertEquals("org.springframework.core.Consumer<? super java.lang.Number>", returnGenericParameterTypeBoundName("suspendFun7"))

		assertEquals(Object::class.java, returnParameterType("suspendFun8"))
		assertEquals(Object::class.java, returnGenericParameterType("suspendFun8"))
	}

	private fun returnParameterType(funName: String) = returnMethodParameter(funName).parameterType
	private fun returnGenericParameterType(funName: String) = returnMethodParameter(funName).genericParameterType
	private fun returnGenericParameterTypeName(funName: String) = returnGenericParameterType(funName).typeName
	private fun returnGenericParameterTypeBoundName(funName: String) = (returnGenericParameterType(funName) as TypeVariable<*>).bounds[0].typeName

	private fun returnMethodParameter(funName: String) =
		MethodParameter(this::class.declaredFunctions.first { it.name == funName }.javaMethod!!, -1)

	@Suppress("unused_parameter")
	fun nullable(nullable: String?): Int? = 42

	@Suppress("unused_parameter")
	fun nonNullable(nonNullable: String): Int = 42

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
