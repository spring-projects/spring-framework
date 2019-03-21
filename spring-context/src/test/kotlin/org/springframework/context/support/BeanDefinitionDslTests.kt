/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support

import org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.context.support.BeanDefinitionDsl.*
import org.springframework.core.env.SimpleCommandLinePropertySource
import org.springframework.core.env.get
import org.springframework.mock.env.MockPropertySource
import java.util.stream.Collectors

@Suppress("UNUSED_EXPRESSION")
class BeanDefinitionDslTests {
	
	@Test
	fun `Declare beans with the functional Kotlin DSL`() {
		val beans = beans {
			bean<Foo>()
			bean<Bar>("bar", scope = Scope.PROTOTYPE)
			bean { Baz(ref("bar")) }
		}

		val context = GenericApplicationContext().apply {
			beans.initialize(this)
			refresh()
		}
		
		assertNotNull(context.getBean<Foo>())
		assertNotNull(context.getBean<Bar>("bar"))
		assertTrue(context.isPrototype("bar"))
		assertNotNull(context.getBean<Baz>())
	}

	@Test
	fun `Declare beans using profile condition with the functional Kotlin DSL`() {
		val beans = beans {
			profile("foo") {
				bean<Foo>()
				profile("bar") {
					bean<Bar>("bar")
				}
			}
			profile("baz") {
				bean { Baz(ref("bar")) }
			}
		}

		val context = GenericApplicationContext().apply {
			environment.addActiveProfile("foo")
			environment.addActiveProfile("bar")
			beans.initialize(this)
			refresh()
		}

		assertNotNull(context.getBean<Foo>())
		assertNotNull(context.getBean<Bar>("bar"))
		try { 
			context.getBean<Baz>()
			fail("Expect NoSuchBeanDefinitionException to be thrown")
		}
		catch(ex: NoSuchBeanDefinitionException) { null }
	}

	@Test
	fun `Declare beans using environment condition with the functional Kotlin DSL`() {
		val beans = beans {
			bean<Foo>()
			bean<Bar>("bar")
			environment( { env["name"].equals("foofoo") } ) {
				bean { FooFoo(env["name"]!!) }
			}
			environment( { activeProfiles.contains("baz") } ) {
				bean { Baz(ref("bar")) }
			}
		}

		val context = GenericApplicationContext().apply {
			environment.propertySources.addFirst(SimpleCommandLinePropertySource("--name=foofoo"))
			beans.initialize(this)
			refresh()
		}

		assertNotNull(context.getBean<Foo>())
		assertNotNull(context.getBean<Bar>("bar"))
		assertEquals("foofoo", context.getBean<FooFoo>().name)
		try {
			context.getBean<Baz>()
			fail("Expect NoSuchBeanDefinitionException to be thrown")
		}
		catch(ex: NoSuchBeanDefinitionException) { null }
	}

	@Test  // SPR-16412
	fun `Declare beans depending on environment properties`() {
		val beans = beans {
			val n = env["number-of-beans"]!!.toInt()
			for (i in 1..n) {
				bean("string$i") { Foo() }
			}
		}

		val context = GenericApplicationContext().apply {
			environment.propertySources.addLast(MockPropertySource().withProperty("number-of-beans", 5))
			beans.initialize(this)
			refresh()
		}

		for (i in 1..5) {
			assertNotNull(context.getBean("string$i"))
		}
	}

	@Test  // SPR-17352
	fun `Retrieve multiple beans via a bean provider`() {
		val beans = beans {
			bean<Foo>()
			bean<Foo>()
			bean { BarBar(provider<Foo>().stream().collect(Collectors.toList())) }
		}

		val context = GenericApplicationContext().apply {
			beans.initialize(this)
			refresh()
		}

		val barbar = context.getBean<BarBar>()
		assertEquals(2, barbar.foos.size)
	}

	@Test  // SPR-17292
	fun `Declare beans leveraging constructor injection`() {
		val beans = beans {
			bean<Bar>()
			bean<Baz>()
		}
		val context = GenericApplicationContext().apply {
			beans.initialize(this)
			refresh()
		}
		context.getBean<Baz>()
	}
	
}

class Foo
class Bar
class Baz(val bar: Bar)
class FooFoo(val name: String)
class BarBar(val foos: Collection<Foo>)
