/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory.support

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanWrapper
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.testfixture.beans.factory.generator.factory.KotlinFactory

class ConstructorResolverKotlinTests {

	@Test
	fun instantiateBeanInstanceWithBeanClassAndFactoryMethodName() {
		val beanFactory = DefaultListableBeanFactory()
		val beanDefinition: BeanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(KotlinFactory::class.java).setFactoryMethod("create")
			.beanDefinition
		val beanWrapper = instantiate(beanFactory, beanDefinition)
		Assertions.assertThat(beanWrapper.wrappedInstance).isEqualTo("test")
	}

	@Test
	fun instantiateBeanInstanceWithBeanClassAndSuspendingFactoryMethodName() {
		val beanFactory = DefaultListableBeanFactory()
		val beanDefinition: BeanDefinition = BeanDefinitionBuilder
			.rootBeanDefinition(KotlinFactory::class.java).setFactoryMethod("suspendingCreate")
			.beanDefinition
		Assertions.assertThatThrownBy { instantiate(beanFactory, beanDefinition, null) }
			.isInstanceOf(BeanCreationException::class.java)
			.hasMessageContaining("suspending functions are not supported")

	}

	private fun instantiate(beanFactory: DefaultListableBeanFactory, beanDefinition: BeanDefinition,
							vararg explicitArgs: Any?): BeanWrapper {
		return ConstructorResolver(beanFactory)
			.instantiateUsingFactoryMethod("testBean", (beanDefinition as RootBeanDefinition), explicitArgs)
	}
}
