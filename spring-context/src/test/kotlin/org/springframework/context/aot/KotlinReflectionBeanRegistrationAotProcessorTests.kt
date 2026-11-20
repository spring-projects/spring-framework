/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.aot

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import org.springframework.aot.test.generate.TestGenerationContext
import org.springframework.beans.factory.aot.*
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RegisteredBean
import org.springframework.beans.factory.support.RootBeanDefinition

/**
 * Tests for [KotlinReflectionBeanRegistrationAotProcessor].
 *
 * @author Sebastien Deleuze
 */
class KotlinReflectionBeanRegistrationAotProcessorTests {

	private val processor = KotlinReflectionBeanRegistrationAotProcessor()

	private val generationContext = TestGenerationContext()

	@Test
	fun processorIsRegistered() {
		assertThat(
			AotServices.factories(javaClass.classLoader).load(BeanRegistrationAotProcessor::class.java))
			.anyMatch(KotlinReflectionBeanRegistrationAotProcessor::class.java::isInstance)
	}

	@Test
	fun shouldProcessKotlinBean() {
		process(SampleKotlinBean::class.java)
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleKotlinBean::class.java))
			.accepts(generationContext.runtimeHints)
		assertThat(
			RuntimeHintsPredicates.reflection().onType(BaseKotlinBean::class.java))
			.accepts(generationContext.runtimeHints)
	}

	@Test
	fun shouldNotProcessJavaBean() {
		process(SampleJavaBean::class.java)
		assertThat(generationContext.runtimeHints.reflection().typeHints()).isEmpty()
	}

	@Test
	fun shouldGenerateOuterClassHints() {
		process(OuterBean.NestedBean::class.java)
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(OuterBean.NestedBean::class.java)
				.and(RuntimeHintsPredicates.reflection().onType(OuterBean::class.java))
		).accepts(generationContext.runtimeHints)
	}

	private fun process(beanClass: Class<*>) {
		createContribution(beanClass)?.applyTo(generationContext, Mockito.mock(BeanRegistrationCode::class.java))
	}

	private fun createContribution(beanClass: Class<*>): BeanRegistrationAotContribution? {
		val beanFactory = DefaultListableBeanFactory()
		beanFactory.registerBeanDefinition(beanClass.name, RootBeanDefinition(beanClass))
		return processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.name))
	}


	class SampleKotlinBean : BaseKotlinBean() {
		fun sample() {
		}
	}

	open class BaseKotlinBean {
		fun base() {
		}
	}

	class OuterBean {
		class NestedBean
	}

}
