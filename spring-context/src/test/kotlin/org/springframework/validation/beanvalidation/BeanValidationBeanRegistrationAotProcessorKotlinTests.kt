/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.beanvalidation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.aot.generate.GenerationContext
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import org.springframework.aot.test.generate.TestGenerationContext
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RegisteredBean
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.validation.beanvalidation.BeanValidationBeanRegistrationAotProcessorTests.*

/**
 * Kotlin tests for [BeanValidationBeanRegistrationAotProcessor].
 *
 * @author Sebastien Deleuze
 */
class BeanValidationBeanRegistrationAotProcessorKotlinTests {

	private val processor = BeanValidationBeanRegistrationAotProcessor()

	private val generationContext: GenerationContext = TestGenerationContext()

	@Test
	fun shouldProcessMethodParameterLevelConstraint() {
		process(MethodParameterLevelConstraint::class.java)
		Assertions.assertThat(
			RuntimeHintsPredicates.reflection().onType(ExistsValidator::class.java)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
		).accepts(generationContext.runtimeHints)
	}

	@Test
	fun shouldSkipMethodParameterLevelConstraintWihExtension() {
		process(MethodParameterLevelConstraintWithExtension::class.java)
		Assertions.assertThat(generationContext.runtimeHints.reflection().typeHints()).isEmpty()
	}

	private fun process(beanClass: Class<*>) {
		val contribution = createContribution(beanClass)
		contribution?.applyTo(generationContext, Mockito.mock())
	}

	private fun createContribution(beanClass: Class<*>): BeanRegistrationAotContribution? {
		val beanFactory = DefaultListableBeanFactory()
		beanFactory.registerBeanDefinition(beanClass.name, RootBeanDefinition(beanClass))
		return processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.name))
	}

	internal class MethodParameterLevelConstraintWithExtension {

		@Suppress("unused")
		fun hello(name: @Exists String): String {
			return name.toHello()
		}

		private fun String.toHello() =
			"Hello $this"
	}

}
