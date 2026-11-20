/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	fun unitReturnType() {
		val method = ::unit.javaMethod!!
		val nullness = Nullness.forMethodReturnType(method)
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED)
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

	@Test
	fun nullableDataClassGetter() {
		val method = NullableName::class.java.getDeclaredMethod("getName")
		val nullness = Nullness.forMethodReturnType(method)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE)
	}

	@Test
	fun nonNullableDataClassGetter() {
		val method = NonNullableName::class.java.getDeclaredMethod("getName")
		val nullness = Nullness.forMethodReturnType(method)
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL)
	}

	@Test
	fun nullableDataClassSetter() {
		val method = NullableName::class.java.getDeclaredMethod("setName", String::class.java)
		val nullness = Nullness.forParameter(method.parameters[0])
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE)
	}

	@Test
	fun nonNullableDataClassSetter() {
		val method = NonNullableName::class.java.getDeclaredMethod("setName", String::class.java)
		val nullness = Nullness.forParameter(method.parameters[0])
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL)
	}

	@Suppress("unused_parameter")
	fun nullable(nullable: String?): String? = "foo"

	@Suppress("unused_parameter")
	fun nonNull(nonNull: String): String = "foo"

	fun unit() {
	}

	data class NullableName(var name: String?)

	data class NonNullableName(var name: String)

}