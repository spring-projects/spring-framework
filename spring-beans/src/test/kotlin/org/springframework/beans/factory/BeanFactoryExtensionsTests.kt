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

package org.springframework.beans.factory

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for BeanFactory Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class BeanFactoryExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var bf: BeanFactory

	@Test
	fun `getBean with reified type parameters`() {
		bf.getBean<Foo>()
		verify(bf, times(1)).getBean(Foo::class.java)
	}

	@Test
	fun `getBean with String and reified type parameters`() {
		val name = "foo"
		bf.getBean<Foo>(name)
		verify(bf, times(1)).getBean(name, Foo::class.java)
	}

	@Test
	fun `getBean with reified type parameters and varargs`() {
		val arg1 = "arg1"
		val arg2 = "arg2"
		bf.getBean<Foo>(arg1, arg2)
		verify(bf, times(1)).getBean(Foo::class.java, arg1, arg2)
	}

	class Foo
}
