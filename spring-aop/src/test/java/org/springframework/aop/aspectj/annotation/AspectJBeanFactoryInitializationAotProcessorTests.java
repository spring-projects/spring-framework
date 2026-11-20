/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

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
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(TestAspect.class, "alterReturnValue"))
				.accepts(this.generationContext.getRuntimeHints());
	}

	private void process(Class<?> beanClass) {
		BeanFactoryInitializationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	private static @Nullable BeanFactoryInitializationAotContribution createContribution(Class<?> beanClass) {
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
