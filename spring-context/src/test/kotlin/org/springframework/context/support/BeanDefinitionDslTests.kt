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

package org.springframework.context.support

import org.junit.Assert.*
import org.junit.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.context.support.BeanDefinitionDsl.*
import org.springframework.core.env.SimpleCommandLinePropertySource
import org.springframework.core.env.get

@Suppress("UNUSED_EXPRESSION")
class BeanDefinitionDslTests {
	
	@Test
	fun `Declare beans with the functional Kotlin DSL`() {
		val beans = beans { 
			bean<Foo>()
			bean<Bar>("bar", scope = Scope.PROTOTYPE)
			bean { Baz(ref()) }
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
			bean<Foo>()
			bean<Bar>("bar")
			profile("baz") {
				profile("pp") {
					bean<Foo>()
				}
				bean { Baz(ref()) }
				bean { Baz(ref("bar")) }
			}
		}

		val context = GenericApplicationContext().apply {
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
			bean { FooFoo(env["name"]) }
			environment( { activeProfiles.contains("baz") } ) {
				bean { Baz(ref()) }
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
		try {
			context.getBean<Baz>()
			fail("Expect NoSuchBeanDefinitionException to be thrown")
		}
		catch(ex: NoSuchBeanDefinitionException) { null }
		val foofoo = context.getBean<FooFoo>()
		assertEquals("foofoo", foofoo.name)
	}
	
}

class Foo
class Bar
class Baz(val bar: Bar)
class FooFoo(val name: String)
