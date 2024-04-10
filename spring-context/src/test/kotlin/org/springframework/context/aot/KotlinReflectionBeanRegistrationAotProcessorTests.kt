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
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(SampleKotlinBean::class.java)
				.withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS)
		).accepts(generationContext.runtimeHints)
		assertThat(
			RuntimeHintsPredicates.reflection()
				.onType(BaseKotlinBean::class.java)
				.withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS)
		).accepts(generationContext.runtimeHints)
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
				.withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS)
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
