/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
