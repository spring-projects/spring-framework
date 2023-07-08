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

package org.springframework.context.annotation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException

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

	class Foo

	class Bar(val foo: Foo)
}
