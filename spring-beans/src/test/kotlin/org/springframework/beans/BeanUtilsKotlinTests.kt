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
 * Kotlin tests for {@link BeanUtils}
 * 
 * @author Sebastien Deleuze
 */
class BeanUtilsKotlinTests {

	@Test
	fun `Instantiate immutable class`() {
		val constructor = BeanUtils.findPrimaryConstructor(Foo::class.java)
		val foo = BeanUtils.instantiateClass(constructor, "bar", 3) as Foo
		assertEquals("bar", foo.param1)
		assertEquals(3, foo.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and all parameters specified`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)
		val bar = BeanUtils.instantiateClass(constructor, "baz", 8) as Bar
		assertEquals("baz", bar.param1)
		assertEquals(8, bar.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter and only mandatory parameters specified by position`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)
		val bar = BeanUtils.instantiateClass(constructor, "baz") as Bar
		assertEquals("baz", bar.param1)
		assertEquals(12, bar.param2)
	}

	@Test
	fun `Instantiate immutable class with optional parameter specified with null value`() {
		val constructor = BeanUtils.findPrimaryConstructor(Bar::class.java)
		val bar = BeanUtils.instantiateClass(constructor, "baz", null) as Bar
		assertEquals("baz", bar.param1)
		assertEquals(12, bar.param2)
	}

	class Foo(val param1: String, val param2: Int)

	class Bar(val param1: String, val param2: Int = 12)
	
}
