package org.springframework.beans.factory

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Mock object based tests for BeanFactory Kotlin extensions
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
