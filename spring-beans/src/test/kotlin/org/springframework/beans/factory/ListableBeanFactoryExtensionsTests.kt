/*
 * Copyright 2002-2019 the original author or authors.
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
import kotlin.reflect.full.createInstance

/**
 * Mock object based tests for ListableBeanFactory Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class ListableBeanFactoryExtensionsTests {

	val lbf = mockk<ListableBeanFactory>(relaxed = true)

	@Test
	fun `getBeanNamesForType with reified type parameters`() {
		lbf.getBeanNamesForType<Foo>()
		verify { lbf.getBeanNamesForType(Foo::class.java, true , true) }
	}

	@Test
	fun `getBeanNamesForType with reified type parameters and Boolean`() {
		lbf.getBeanNamesForType<Foo>(false)
		verify { lbf.getBeanNamesForType(Foo::class.java, false , true) }
	}

	@Test
	fun `getBeanNamesForType with reified type parameters, Boolean and Boolean`() {
		lbf.getBeanNamesForType<Foo>(false, false)
		verify { lbf.getBeanNamesForType(Foo::class.java, false , false) }
	}

	@Test
	fun `getBeansOfType with reified type parameters`() {
		lbf.getBeansOfType<Foo>()
		verify { lbf.getBeansOfType(Foo::class.java, true , true) }
	}

	@Test
	fun `getBeansOfType with reified type parameters and Boolean`() {
		lbf.getBeansOfType<Foo>(false)
		verify { lbf.getBeansOfType(Foo::class.java, false , true) }
	}

	@Test
	fun `getBeansOfType with reified type parameters, Boolean and Boolean`() {
		lbf.getBeansOfType<Foo>(false, false)
		verify { lbf.getBeansOfType(Foo::class.java, false , false) }
	}

	@Test
	fun `getBeanNamesForAnnotation with reified type parameters`() {
		lbf.getBeanNamesForAnnotation<Bar>()
		verify { lbf.getBeanNamesForAnnotation(Bar::class.java) }
	}

	@Test
	fun `getBeansWithAnnotation with reified type parameters`() {
		lbf.getBeansWithAnnotation<Bar>()
		verify { lbf.getBeansWithAnnotation(Bar::class.java) }
	}

	@Suppress("UNUSED_VARIABLE")
	@Test
	fun `findAnnotationOnBean with String and reified type parameters`() {
		val name = "bar"
		every { lbf.findAnnotationOnBean(name, Bar::class.java) } returns Bar::class.createInstance()
		val annotation: Bar? = lbf.findAnnotationOnBean(name)
		verify { lbf.findAnnotationOnBean(name, Bar::class.java) }
	}

	class Foo

	annotation class Bar
}
