/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link AspectJAdvisorBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 * @since 6.1
 */
class AspectJAdvisorBeanRegistrationAotProcessorTests {

	private final GenerationContext generationContext = new TestGenerationContext();

	private final RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();


	@Test
	void shouldProcessAspectJClass() {
		process(AspectJClass.class);
		assertThat(reflection().onType(AspectJClass.class).withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS))
				.accepts(this.runtimeHints);
	}

	@Test
	void shouldSkipRegularClass() {
		process(RegularClass.class);
		assertThat(this.runtimeHints.reflection().typeHints()).isEmpty();
	}

	void process(Class<?> beanClass) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	private static @Nullable BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return new AspectJAdvisorBeanRegistrationAotProcessor()
				.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}


	static class AspectJClass {
		private static java.lang.Throwable ajc$initFailureCause;
	}

	static class RegularClass {
		private static java.lang.Throwable initFailureCause;
	}

}
