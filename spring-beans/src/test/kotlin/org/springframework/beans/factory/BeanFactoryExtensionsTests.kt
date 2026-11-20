/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType

/**
 * Mock object based tests for BeanFactory Kotlin extensions.
 *
 * @author Sebastien Deleuze
 * @author Yanming Zhou
 */
class BeanFactoryExtensionsTests {

	val bf = mockk<BeanFactory>(relaxed = true)

	@Test
	fun `getBean with reified type parameters`() {
		val foo = Foo()
		every { bf.getBeanProvider<Foo>(ofType<ResolvableType>()).getObject() } returns foo
		assertThat(bf.getBean<Foo>()).isSameAs(foo)
		verify { bf.getBeanProvider<ObjectProvider<Foo>>(ofType<ResolvableType>()).getObject() }
	}

	@Test
	fun `getBean with reified generic type parameters`() {
		val foo = listOf(Foo())
		every { bf.getBeanProvider<List<Foo>>(ofType<ResolvableType>()).getObject() } returns foo
		assertThat(bf.getBean<List<Foo>>()).isSameAs(foo)
		verify { bf.getBeanProvider<ObjectProvider<List<Foo>>>(ofType<ResolvableType>()).getObject() }
	}

	@Test
	fun `getBean with String and reified type parameters`() {
		val name = "foo"
		bf.getBean<Foo>(name)
		verify { bf.getBean(name, ofType<ParameterizedTypeReference<Foo>>()) }
	}

	@Test
	fun `getBean with String and reified generic type parameters`() {
		val name = "foo"
		val foo = listOf(Foo())
		every { bf.getBean(name, ofType<ParameterizedTypeReference<List<Foo>>>()) } returns foo
		assertThat(bf.getBean<List<Foo>>("foo")).isSameAs(foo)
		verify { bf.getBean(name, ofType<ParameterizedTypeReference<List<Foo>>>()) }
	}

	@Test
	fun `getBean with reified type parameters and varargs`() {
		val arg1 = "arg1"
		val arg2 = "arg2"
		val bar = Bar(arg1, arg2)
		every { bf.getBeanProvider<Bar>(ofType<ResolvableType>()).getObject(arg1, arg2) } returns bar
		assertThat(bf.getBean<Bar>(arg1, arg2)).isSameAs(bar)
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
