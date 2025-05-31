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

package org.springframework.beans

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [BeanUtils].
 * 
 * @author Sebastien Deleuze
 */
@Suppress("unused", "UNUSED_PARAMETER")
class BeanUtilsKotlinTests {

	@Test
	fun `Instantiate immutable class`() {
		val constructor = BeanUtils.findPrimaryConstructor(Foo::class.java)!!
		val foo = BeanUtils.instantiateClass(constructor, "a", 3)
		assertThat(foo.param1).isEqualTo("a")
		assertThat(foo.param2).isEqualTo(3)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and all parameters specified`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", 8)
		assertThat(bar.param1).isEqualTo("a")
		assertThat(bar.param2).isEqualTo(8)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and only mandatory parameters specified by position`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a")
		assertThat(bar.param1).isEqualTo("a")
		assertThat(bar.param2).isEqualTo(12)
	}

	@Test
	fun `Instantiate immutable class with optional parameter specified with null value`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", null)
		assertThat(bar.param1).isEqualTo("a")
		assertThat(bar.param2).isEqualTo(12)
	}

	@Test  // gh-22531
	fun `Instantiate immutable class with nullable parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(Qux::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", null)
		assertThat(bar.param1).isEqualTo("a")
		assertThat(bar.param2).isNull()
	}

	@Test  // SPR-15851
	fun `Instantiate mutable class with declared constructor and default values for all parameters`() {
		val baz = BeanUtils.instantiateClass(Baz::class.java.getDeclaredConstructor())
		assertThat(baz.param1).isEqualTo("a")
		assertThat(baz.param2).isEqualTo(12)
	}

	@Test
	@Suppress("UsePropertyAccessSyntax")
	fun `Instantiate class with private constructor`() {
		BeanUtils.instantiateClass(PrivateConstructor::class.java.getDeclaredConstructor())
	}

	@Test
	fun `Instantiate class with protected constructor`() {
		BeanUtils.instantiateClass(ProtectedConstructor::class.java.getDeclaredConstructor())
	}

	@Test
	fun `Instantiate private class`() {
		BeanUtils.instantiateClass(PrivateClass::class.java.getDeclaredConstructor())
	}

	@Test
	fun `Instantiate value class`() {
		val constructor = BeanUtils.findPrimaryConstructor(ValueClass::class.java)!!
		val value = "Hello value class!"
		val instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ValueClass(value))
	}

	@Test
	fun `Instantiate value class with multiple constructors`() {
		val constructor = BeanUtils.findPrimaryConstructor(ValueClassWithMultipleConstructors::class.java)!!
		val value = "Hello value class!"
		val instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ValueClassWithMultipleConstructors(value))
	}

	@Test
	fun `Instantiate class with value class parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(ConstructorWithValueClass::class.java)!!
		val value = ValueClass("Hello value class!")
		val instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ConstructorWithValueClass(value))
	}

	@Test
	fun `Instantiate class with nullable value class parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(ConstructorWithNullableValueClass::class.java)!!
		val value = ValueClass("Hello value class!")
		var instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ConstructorWithNullableValueClass(value))
		instance = BeanUtils.instantiateClass(constructor, null)
		assertThat(instance).isEqualTo(ConstructorWithNullableValueClass(null))
	}

	@Test
	fun `Instantiate primitive value class`() {
		val constructor = BeanUtils.findPrimaryConstructor(PrimitiveValueClass::class.java)!!
		val value = 0
		val instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(PrimitiveValueClass(value))
	}

	@Test
	fun `Instantiate class with primitive value class parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(ConstructorWithPrimitiveValueClass::class.java)!!
		val value = PrimitiveValueClass(0)
		val instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ConstructorWithPrimitiveValueClass(value))
	}

	@Test
	fun `Instantiate class with nullable primitive value class parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(ConstructorWithNullablePrimitiveValueClass::class.java)!!
		val value = PrimitiveValueClass(0)
		var instance = BeanUtils.instantiateClass(constructor, value)
		assertThat(instance).isEqualTo(ConstructorWithNullablePrimitiveValueClass(value))
		instance = BeanUtils.instantiateClass(constructor, null)
		assertThat(instance).isEqualTo(ConstructorWithNullablePrimitiveValueClass(null))
	}

	@Test
	fun `Get parameter names with Foo`() {
		val ctor = BeanUtils.findPrimaryConstructor(Foo::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).containsExactly("param1", "param2")
	}

	@Test
	fun `Get parameter names filters out DefaultConstructorMarker with ConstructorWithValueClass`() {
		val ctor = BeanUtils.findPrimaryConstructor(ConstructorWithValueClass::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).containsExactly("value")
	}

	@Test
	fun `getParameterNames filters out DefaultConstructorMarker with ConstructorWithNullableValueClass`() {
		val ctor = BeanUtils.findPrimaryConstructor(ConstructorWithNullableValueClass::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).containsExactly("value")
	}

	@Test
	fun `getParameterNames filters out DefaultConstructorMarker with ConstructorWithPrimitiveValueClass`() {
		val ctor = BeanUtils.findPrimaryConstructor(ConstructorWithPrimitiveValueClass::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).containsExactly("value")
	}

	@Test
	fun `getParameterNames filters out DefaultConstructorMarker with ConstructorWithNullablePrimitiveValueClass`() {
		val ctor = BeanUtils.findPrimaryConstructor(ConstructorWithNullablePrimitiveValueClass::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).containsExactly("value")
	}

	@Test
	fun `getParameterNames with ClassWithZeroParameterCtor`() {
		val ctor = BeanUtils.findPrimaryConstructor(ClassWithZeroParameterCtor::class.java)!!
		val names = BeanUtils.getParameterNames(ctor)
		assertThat(names).isEmpty()
	}


	class Foo(val param1: String, val param2: Int)

	class Bar(val param1: String, val param2: Int = 12)

	class Baz(var param1: String = "a", var param2: Int = 12)

	class Qux(val param1: String, val param2: Int?)

	class TwoConstructorsWithDefaultOne {

		constructor()

		constructor(param1: String)
	}

	class TwoConstructorsWithoutDefaultOne {

		constructor(param1: String)

		constructor(param1: String, param2: String)
	}

	class OneConstructorWithDefaultOne {

		constructor()
	}

	class OneConstructorWithoutDefaultOne {

		constructor(param1: String)
	}

	class PrivateConstructor private constructor()

	open class ProtectedConstructor protected constructor()

	private class PrivateClass

	@JvmInline
	value class ValueClass(private val value: String)

	@JvmInline
	value class ValueClassWithMultipleConstructors(private val value: String) {
		constructor() : this("Fail")
		constructor(part1: String, part2: String) : this("Fail")
	}

	data class ConstructorWithValueClass(val value: ValueClass)

	data class ConstructorWithNullableValueClass(val value: ValueClass?)

	@JvmInline
	value class PrimitiveValueClass(private val value: Int)

	data class ConstructorWithPrimitiveValueClass(val value: PrimitiveValueClass)

	data class ConstructorWithNullablePrimitiveValueClass(val value: PrimitiveValueClass?)

	class ClassWithZeroParameterCtor()

}
