/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans

import org.junit.Assert.*
import org.junit.Test

/**
 * Kotlin tests for {@link BeanUtils}.
 * 
 * @author Sebastien Deleuze
 */
@Suppress("unused", "UNUSED_PARAMETER")
class KotlinBeanUtilsTests {

	@Test
	fun `Instantiate immutable class`() {
		val constructor = BeanUtils.findPrimaryConstructor(Foo::class.java)!!
		val foo = BeanUtils.instantiateClass(constructor, "a", 3)
		assertEquals("a", foo.param1)
		assertEquals(3, foo.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and all parameters specified`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", 8)
		assertEquals("a", bar.param1)
		assertEquals(8, bar.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and only mandatory parameters specified by position`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a")
		assertEquals("a", bar.param1)
		assertEquals(12, bar.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter specified with null value`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", null)
		assertEquals("a", bar.param1)
		assertEquals(12, bar.param2)
	}

	@Test  // gh-22531
	fun `Instantiate immutable class with nullable parameter`() {
		val constructor = BeanUtils.findPrimaryConstructor(Qux::class.java)!!
		val bar = BeanUtils.instantiateClass(constructor, "a", null)
		assertEquals("a", bar.param1)
		assertNull(bar.param2)
	}

	@Test  // SPR-15851
	fun `Instantiate mutable class with declared constructor and default values for all parameters`() {
		val baz = BeanUtils.instantiateClass(Baz::class.java.getDeclaredConstructor())
		assertEquals("a", baz.param1)
		assertEquals(12, baz.param2)
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

}
