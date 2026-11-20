/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.service.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.DecoratingProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.proxies;

/**
 * Tests for {@link HttpExchangeBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
class HttpExchangeBeanRegistrationAotProcessorTests {

	private final GenerationContext generationContext = new TestGenerationContext();

	private final RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();


	@Test
	void shouldSkipNonAnnotatedInterface() {
		process(NonAnnotatedInterface.class);
		assertThat(this.runtimeHints.proxies().jdkProxyHints()).isEmpty();
	}

	@Test
	void shouldProcessAnnotatedInterface() {
		process(AnnotatedInterface.class);
		assertThat(proxies().forInterfaces(AnnotatedInterface.class, SpringProxy.class, Advised.class, DecoratingProxy.class))
				.accepts(this.runtimeHints);
	}


	private void process(Class<?> beanClass) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	private static @Nullable BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return new HttpExchangeBeanRegistrationAotProcessor()
				.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}


	interface NonAnnotatedInterface {

		void notExchange();
	}

	interface AnnotatedInterface {

		@GetExchange
		void get();
	}

}
