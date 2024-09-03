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

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AspectJBeanFactoryInitializationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
class AspectJBeanFactoryInitializationAotProcessorTests {

	private final GenerationContext generationContext = new TestGenerationContext();

	@Test
	void shouldSkipEmptyClass() {
		assertThat(createContribution(EmptyClass.class)).isNull();
	}

	@Test
	void shouldProcessAspect() {
		process(TestAspect.class);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(TestAspect.class, "alterReturnValue").invoke())
				.accepts(this.generationContext.getRuntimeHints());
	}

	private void process(Class<?> beanClass) {
		BeanFactoryInitializationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	@Nullable
	private static BeanFactoryInitializationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return new AspectJBeanFactoryInitializationAotProcessor().processAheadOfTime(beanFactory);
	}


	static class EmptyClass { }

	@Aspect
	static class TestAspect {

		@Around("pointcut()")
		public Object alterReturnValue(ProceedingJoinPoint joinPoint) throws Throwable {
			joinPoint.proceed();
			return "A-from-aspect";
		}

		@Pointcut("execution(* com.example.aspect.Test*.methodA(..))")
		private void pointcut() {
		}

	}

}
