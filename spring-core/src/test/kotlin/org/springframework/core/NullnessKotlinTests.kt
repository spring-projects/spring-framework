/*
 * Copyright 2002-2025 the original author or authors.
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

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.reflect.jvm.javaMethod

/**
 * Kotlin tests for [Nullness].
 *
 * @author Sebastien Deleuze
 */
class NullnessKotlinTests {

	val nullableProperty: String? = ""
	val nonNullProperty: String = ""

	@Test
	fun nullableReturnType() {
		val method = ::nullable.javaMethod!!
		val nullness = Nullness.forMethodReturnType(method)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE)
	}

	@Test
	fun nullableParameter() {
		val method = ::nullable.javaMethod!!
		val nullness = Nullness.forParameter(method.parameters[0])
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE)
	}

	@Test
	fun nonNullReturnType() {
		val method = ::nonNull.javaMethod!!
		val nullness = Nullness.forMethodReturnType(method)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL)
	}

	@Test
	fun nonNullParameter() {
		val method = ::nonNull.javaMethod!!
		val nullness = Nullness.forParameter(method.parameters[0])
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL)
	}

	@Test
	fun nullableProperty() {
		val field = javaClass.getDeclaredField("nullableProperty")
		val nullness = Nullness.forField(field)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE)
	}

	@Test
	fun nonNullProperty() {
		val field = javaClass.getDeclaredField("nonNullProperty")
		val nullness = Nullness.forField(field)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL)
	}

	@Suppress("unused_parameter")
	fun nullable(nullable: String?): String? = "foo"

	@Suppress("unused_parameter")
	fun nonNull(nonNull: String): String = "foo"

}