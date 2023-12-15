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

package org.springframework.beans.factory

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType

/**
 * Mock object based tests for BeanFactory Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class BeanFactoryExtensionsTests {

	val bf = mockk<BeanFactory>(relaxed = true)

	@Test
	fun `getBean with reified type parameters`() {
		val foo = Foo()
		every { bf.getBeanProvider<Foo>(ofType<ResolvableType>()).getObject() } returns foo
		bf.getBean<Foo>()
		verify { bf.getBeanProvider<ObjectProvider<Foo>>(ofType<ResolvableType>()).getObject() }
	}

	@Test
	fun `getBean with String and reified type parameters`() {
		val name = "foo"
		bf.getBean<Foo>(name)
		verify { bf.getBean(name, Foo::class.java) }
	}

	@Test
	fun `getBean with reified type parameters and varargs`() {
		val arg1 = "arg1"
		val arg2 = "arg2"
		val bar = Bar(arg1, arg2)
		every { bf.getBeanProvider<Bar>(ofType<ResolvableType>()).getObject(arg1, arg2) } returns bar
		bf.getBean<Bar>(arg1, arg2)
		verify { bf.getBeanProvider<Bar>(ofType<ResolvableType>()).getObject(arg1, arg2) }
	}

	@Test
	fun `getBeanProvider with reified type parameters`() {
		bf.getBeanProvider<Foo>()
		verify { bf.getBeanProvider<ObjectProvider<Foo>>(ofType<ResolvableType>()) }
	}

	class Foo

	@Suppress("UNUSED_PARAMETER")
	class Bar(arg1: String, arg2: String)
}
