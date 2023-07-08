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
