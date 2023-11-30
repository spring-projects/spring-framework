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

package org.springframework.context.support

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.support.BeanDefinitionDsl.*
import org.springframework.core.Ordered
import org.springframework.core.env.get
import org.springframework.core.testfixture.env.MockPropertySource
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
		
		context.getBean<Foo>()
		context.getBean<Bar>("bar")
		assertThat(context.isPrototype("bar")).isTrue()
		context.getBean<Baz>()
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

		context.getBean<Foo>()
		context.getBean<Bar>("bar")
		assertThatExceptionOfType(NoSuchBeanDefinitionException::class.java).isThrownBy { context.getBean<Baz>() }
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
			environment.propertySources.addFirst(org.springframework.core.env.SimpleCommandLinePropertySource("--name=foofoo"))
			beans.initialize(this)
			refresh()
		}

		context.getBean<Foo>()
		context.getBean<Bar>("bar")
		assertThat(context.getBean<FooFoo>().name).isEqualTo("foofoo")
		assertThatExceptionOfType(NoSuchBeanDefinitionException::class.java).isThrownBy { context.getBean<Baz>() }
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
			context.getBean("string$i")
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
		assertThat(barbar.foos.size).isEqualTo(2)
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

	@Test  // gh-21845
	fun `Declare beans leveraging callable reference`() {
		val beans = beans {
			bean<Bar>()
			bean(::baz)
			bean(::foo)
		}
		val context = GenericApplicationContext().apply {
			beans.initialize(this)
			refresh()
		}
		context.getBean<Baz>()
	}
	

	@Test
	fun `Declare beans with accepted profiles`() {
		val beans = beans {
			profile("foo") { bean<Foo>() }
			profile("!bar") { bean<Bar>() }
			profile("bar | barbar") { bean<BarBar>() }
			profile("baz & buz") { bean<Baz>() }
			profile("baz & foo") { bean<FooFoo>() }
		}
		val context = GenericApplicationContext().apply {
			environment.addActiveProfile("barbar")
			environment.addActiveProfile("baz")
			environment.addActiveProfile("buz")
			beans.initialize(this)
			refresh()
		}
		context.getBean<Baz>()
		context.getBean<BarBar>()
		context.getBean<Bar>()

		try {
			context.getBean<Foo>()
			fail("should have thrown an Exception")
		} catch (ignored: Exception) {
		}
		try {
			context.getBean<FooFoo>()
			fail("should have thrown an Exception")
		} catch (ignored: Exception) {
		}
	}

	@Test
	fun `Declare beans with ordering`() {
		val beans = beans {
			bean<FooFoo>(order = Ordered.LOWEST_PRECEDENCE) {
				FooFoo("lowest")
			}
			bean<FooFoo>(order = Ordered.HIGHEST_PRECEDENCE) {
				FooFoo("highest")
			}
		}

		val context = GenericApplicationContext().apply {
			beans.initialize(this)
			refresh()
		}

		assertThat(context.getBeanProvider<FooFoo>().orderedStream().map { it.name }).containsExactly("highest", "lowest")
	}
}

class Foo
class Bar
class Baz(val bar: Bar)
class FooFoo(val name: String)
class BarBar(val foos: Collection<Foo>)

fun baz(bar: Bar) = Baz(bar)
fun foo() = Foo()
