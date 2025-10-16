/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.annotation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanProvider
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.mock.env.MockEnvironment
import java.util.function.Supplier

/**
 * Kotlin tests leveraging [BeanRegistrarDsl].
 *
 * @author Sebastien Deleuze
 */
class BeanRegistrarDslConfigurationTests {

	@Test
	fun beanRegistrar() {
		val context = AnnotationConfigApplicationContext()
		context.register(BeanRegistrarKotlinConfiguration::class.java)
		context.environment = MockEnvironment().withProperty("hello.world", "Hello World!")
		context.refresh()
		assertThat(context.getBean<Bar>().foo).isEqualTo(context.getBean<Foo>())
		assertThat(context.getBean<Foo>("foo")).isEqualTo(context.getBean<Foo>("fooAlias"))
		assertThatThrownBy { context.getBean<Baz>() }.isInstanceOf(NoSuchBeanDefinitionException::class.java)
		assertThat(context.getBean<Init>().initialized).isTrue()
		val beanDefinition = context.getBeanDefinition("bar")
		assertThat(beanDefinition.scope).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE)
		assertThat(beanDefinition.isLazyInit).isTrue()
		assertThat(beanDefinition.description).isEqualTo("Custom description")
		assertThat(context.getBean<Boo>()).isEqualTo(Boo("booFactory"))
	}

	@Test
	fun beanRegistrarWithProfile() {
		val context = AnnotationConfigApplicationContext()
		context.register(BeanRegistrarKotlinConfiguration::class.java)
		context.environment = MockEnvironment().withProperty("hello.world", "Hello World!")
		context.environment.addActiveProfile("baz")
		context.refresh()
		assertThat(context.getBean<Bar>().foo).isEqualTo(context.getBean<Foo>())
		assertThat(context.getBean<Baz>().message).isEqualTo("Hello World!")
		assertThat(context.getBean<Init>().initialized).isTrue()
	}

	@Test
	fun genericBeanRegistrar() {
		val context = AnnotationConfigApplicationContext(GenericBeanRegistrarKotlinConfiguration::class.java)
		val beanDefinition = context.getBeanDefinition("fooSupplier") as RootBeanDefinition
		assertThat(beanDefinition.resolvableType.resolveGeneric(0)).isEqualTo(Foo::class.java)
	}

	@Test
	fun chainedBeanRegistrar() {
		val context = AnnotationConfigApplicationContext(ChainedBeanRegistrarKotlinConfiguration::class.java)
		assertThat(context.getBean<Bar>().foo).isEqualTo(context.getBean<Foo>())
	}

	@Test
	fun multipleBeanRegistrars() {
		val context = AnnotationConfigApplicationContext(MultipleBeanRegistrarsKotlinConfiguration::class.java)
		assertThat(context.getBeanProvider<Foo>().singleOrNull()).isNotNull
		assertThat(context.getBeanProvider<Bar>().singleOrNull()).isNotNull
	}

	class Foo
	data class Bar(val foo: Foo)
	data class Baz(val message: String = "")
	data class Boo(val message: String = "")
	class Init  : InitializingBean {
		var initialized: Boolean = false

		override fun afterPropertiesSet() {
			initialized = true
		}

	}

	@Configuration
	@Import(value = [FooRegistrar::class, BarRegistrar::class])
	internal class MultipleBeanRegistrarsKotlinConfiguration

	private class FooRegistrar : BeanRegistrarDsl({
		registerBean<Foo>()
	})

	private class BarRegistrar : BeanRegistrarDsl({
		registerBean<Bar>()
	})

	@Configuration
	@Import(SampleBeanRegistrar::class)
	internal class BeanRegistrarKotlinConfiguration

	private class SampleBeanRegistrar : BeanRegistrarDsl({
		registerBean<Foo>("foo")
		registerAlias("foo", "fooAlias")
		registerBean(
			name = "bar",
			prototype = true,
			lazyInit = true,
			description = "Custom description") {
			Bar(bean<Foo>())
		}
		profile("baz") {
			registerBean { Baz(env.getRequiredProperty("hello.world")) }
		}
		registerBean<Init>()
		registerBean(::booFactory, "fooFactory")
	})

	@Configuration
	@Import(GenericBeanRegistrar::class)
	internal class GenericBeanRegistrarKotlinConfiguration

	private class GenericBeanRegistrar : BeanRegistrarDsl({
		registerBean<Supplier<Foo>>(name = "fooSupplier") {
			Supplier<Foo> { Foo() }
		}
	})

	@Configuration
	@Import(ChainedBeanRegistrar::class)
	internal class ChainedBeanRegistrarKotlinConfiguration

	private class ChainedBeanRegistrar : BeanRegistrarDsl({
		register(SampleBeanRegistrar())
	})
}

fun booFactory() = BeanRegistrarDslConfigurationTests.Boo("booFactory")
