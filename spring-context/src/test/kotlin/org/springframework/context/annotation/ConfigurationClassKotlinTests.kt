/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException
import org.springframework.beans.factory.support.DefaultListableBeanFactory

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

	class Foo

	class Bar(val foo: Foo)
}
