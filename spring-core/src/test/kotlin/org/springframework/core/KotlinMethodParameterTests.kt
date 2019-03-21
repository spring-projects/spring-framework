/*
 * Copyright 2002-2018 the original author or authors.
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
import org.junit.Assert.*
import java.lang.reflect.Method

/**
 * Tests for Kotlin support in [MethodParameter].
 *
 * @author Raman Gupta
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
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


	@Suppress("unused_parameter")
	fun nullable(nullable: String?): Int? = 42

	@Suppress("unused_parameter")
	fun nonNullable(nonNullable: String): Int = 42

	inner class InnerClass

	@Suppress("unused_parameter")
	inner class InnerClassWithParameter(nonNullable: String, nullable: String?)

	@Suppress("unused_parameter")
	class RegularClass(nonNullable: String, nullable: String?)

}
