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
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.support.registerBean

/**
 * Integration tests for Kotlin configuration classes.
 *
 * @author Sebastien Deleuze
 */
class ConfigurationClassKotlinTests {

	@Test
	fun `Final configuration with default proxyBeanMethods value`() {
		assertThatExceptionOfType(BeanDefinitionParsingException::class.java).isThrownBy {
			AnnotationConfigApplicationContext(FinalConfigurationWithProxy::class.java)
		}
	}

	@Test
	fun `Final configuration with proxyBeanMethods set to false`() {
		val context = AnnotationConfigApplicationContext(FinalConfigurationWithoutProxy::class.java)
		val foo = context.getBean<Foo>()
		assertThat(context.getBean<Bar>().foo).isEqualTo(foo)
	}

	@Test
	fun `Configuration with @JvmStatic registers a single bean`() {
		val beanFactory = DefaultListableBeanFactory().apply {
			isAllowBeanDefinitionOverriding = false
		}
		val context = AnnotationConfigApplicationContext(beanFactory)
		context.register(ProcessorConfiguration::class.java)
		context.refresh()
	}

	@Test
	fun `Configuration with constructor parameter default value is autowired`() {
		val context = AnnotationConfigApplicationContext()
		context.registerBean<Foo>()
		context.register(ConfigurationWithParameterDefaultValue::class.java)
		context.refresh()
		assertThat(context.getBean<Bar>().foo).isSameAs(context.getBean<Foo>())
	}

	@Test
	fun `Configuration with nullable constructor parameter default value is autowired`() {
		val context = AnnotationConfigApplicationContext()
		context.registerBean<Foo>()
		context.register(ConfigurationWithNullableParameterDefaultValue::class.java)
		context.refresh()
		assertThat(context.getBean<NullableBar>().foo).isSameAs(context.getBean<Foo>())
	}

	@Test
	fun `Configuration with constructor parameter default value falls back to the default value`() {
		val context = AnnotationConfigApplicationContext(ConfigurationWithParameterDefaultValue::class.java)
		assertThat(context.getBeanNamesForType(Foo::class.java)).isEmpty()
		assertThat(context.getBean<Bar>().foo).isNotNull()
	}


	@Configuration
	class FinalConfigurationWithProxy {

		@Bean
		fun foo() = Foo()

		@Bean
		fun bar(foo: Foo) = Bar(foo)
	}

	@Configuration(proxyBeanMethods = false)
	class FinalConfigurationWithoutProxy {

		@Bean
		fun foo() = Foo()

		@Bean
		fun bar(foo: Foo) = Bar(foo)
	}

	@Configuration
	open class ProcessorConfiguration {

		companion object {

			@Bean
			@JvmStatic
			fun processor(): BeanPostProcessor {
				return object: BeanPostProcessor{}
			}
		}
	}

	@Configuration
	open class ConfigurationWithParameterDefaultValue(private val foo: Foo = Foo()) {

		@Bean
		open fun bar() = Bar(foo)
	}

	@Configuration
	open class ConfigurationWithNullableParameterDefaultValue(private val foo: Foo? = null) {

		@Bean
		open fun nullableBar() = NullableBar(foo)
	}

	class Foo

	class Bar(val foo: Foo)

	class NullableBar(val foo: Foo?)
}
