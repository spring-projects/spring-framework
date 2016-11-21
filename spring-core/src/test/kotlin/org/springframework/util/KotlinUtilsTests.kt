package org.springframework.util

import org.junit.Before
import org.junit.Test

import java.lang.reflect.Method

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.springframework.core.MethodParameter
import org.springframework.util.KotlinUtils.*

/**
 * Test fixture for [KotlinUtils].
 *
 * @author Raman Gupta
 * @author Sebastien Deleuze
 */
class KotlinUtilsTests {

	lateinit var nullableMethod: Method

	lateinit var nonNullableMethod: Method

	@Before
	@Throws(NoSuchMethodException::class)
	fun setup() {
		nullableMethod = javaClass.getMethod("nullable", String::class.java)
		nonNullableMethod = javaClass.getMethod("nonNullable", String::class.java)
	}

	@Test
	fun `Is kotlin present`() {
		// we'd have to change the build to test the opposite
		assertTrue(isKotlinPresent())
	}

	@Test
	fun `Are kotlin classes detected`() {
		assertFalse(isKotlinClass(MethodParameter::class.java))
		assertTrue(isKotlinClass(javaClass))
	}

	@Test
	fun `Obtains method return type nullability`() {
		assertTrue(isNullable(MethodParameter(nullableMethod, -1)))
		assertFalse(isNullable(MethodParameter(nonNullableMethod, -1)))
	}

	@Test
	fun `Obtains method parameter nullability`() {
		assertTrue(isNullable(MethodParameter(nullableMethod, 0)))
		assertFalse(isNullable(MethodParameter(nonNullableMethod, 0)))
	}

	@Suppress("unused", "unused_parameter")
	fun nullable(p1: String?): Int? = 42

	@Suppress("unused", "unused_parameter")
	fun nonNullable(p1: String): Int = 42

}
