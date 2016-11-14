package org.springframework.util

import org.junit.Before
import org.junit.Test

import java.lang.reflect.Method

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.springframework.core.MethodParameter
import org.springframework.util.KotlinUtils.*

/**
 * Unit tests for [KotlinUtils].

 * @author Raman Gupta
 */
class KotlinUtilsTests {

	private lateinit var methodNullable: Method
	private lateinit var methodNonNullable: Method

	@Before
	@Throws(NoSuchMethodException::class)
	fun setUp() {
		methodNullable = javaClass.getMethod("methodNullable", String::class.java, java.lang.Long.TYPE)
		methodNonNullable = javaClass.getMethod("methodNonNullable", String::class.java, java.lang.Long.TYPE)
	}

	@Test
	fun `Is kotlin present`() {
		// we'd have to change the build to test the opposite
		assertTrue(isKotlinPresent())
	}

	@Test
	fun `Are kotlin classes detected`() {
		assertFalse(isKotlinClass(null))
		assertFalse(isKotlinClass(MethodParameter::class.java))
		assertTrue(isKotlinClass(javaClass))
	}

	@Test
	fun `Obtains method return type nullability`() {
		assertTrue(isNullable(-1, methodNullable, null))
		assertFalse(isNullable(-1, methodNonNullable, null))
	}

	@Test
	fun `Obtains method parameter nullability`() {
		assertTrue(isNullable(0, methodNullable, null))
		assertFalse(isNullable(1, methodNullable, null))
	}

	@Suppress("unused", "unused_parameter")
	fun methodNullable(p1: String?, p2: Long): Int? = 42

	@Suppress("unused", "unused_parameter")
	fun methodNonNullable(p1: String?, p2: Long): Int = 42
}
