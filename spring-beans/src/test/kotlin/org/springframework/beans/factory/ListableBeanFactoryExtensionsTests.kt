/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for ListableBeanFactory Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class ListableBeanFactoryExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var lbf: ListableBeanFactory

	@Test
	fun `getBeanNamesForType with reified type parameters`() {
		lbf.getBeanNamesForType<Foo>()
		verify(lbf, times(1)).getBeanNamesForType(Foo::class.java, true , true)
	}

	@Test
	fun `getBeanNamesForType with reified type parameters and Boolean`() {
		lbf.getBeanNamesForType<Foo>(false)
		verify(lbf, times(1)).getBeanNamesForType(Foo::class.java, false , true)
	}

	@Test
	fun `getBeanNamesForType with reified type parameters, Boolean and Boolean`() {
		lbf.getBeanNamesForType<Foo>(false, false)
		verify(lbf, times(1)).getBeanNamesForType(Foo::class.java, false , false)
	}

	@Test
	fun `getBeansOfType with reified type parameters`() {
		lbf.getBeansOfType<Foo>()
		verify(lbf, times(1)).getBeansOfType(Foo::class.java, true , true)
	}

	@Test
	fun `getBeansOfType with reified type parameters and Boolean`() {
		lbf.getBeansOfType<Foo>(false)
		verify(lbf, times(1)).getBeansOfType(Foo::class.java, false , true)
	}

	@Test
	fun `getBeansOfType with reified type parameters, Boolean and Boolean`() {
		lbf.getBeansOfType<Foo>(false, false)
		verify(lbf, times(1)).getBeansOfType(Foo::class.java, false , false)
	}

	@Test
	fun `getBeanNamesForAnnotation with reified type parameters`() {
		lbf.getBeanNamesForAnnotation<Bar>()
		verify(lbf, times(1)).getBeanNamesForAnnotation(Bar::class.java)
	}

	@Test
	fun `getBeansWithAnnotation with reified type parameters`() {
		lbf.getBeansWithAnnotation<Bar>()
		verify(lbf, times(1)).getBeansWithAnnotation(Bar::class.java)
	}

	@Test
	fun `findAnnotationOnBean with String and reified type parameters`() {
		val name = "bar"
		lbf.findAnnotationOnBean<Bar>(name)
		verify(lbf, times(1)).findAnnotationOnBean(name, Bar::class.java)
	}

	class Foo

	annotation class Bar
}
