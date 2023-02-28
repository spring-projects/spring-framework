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

package org.springframework.web.service.annotation;

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
import org.springframework.lang.Nullable;

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

	@Nullable
	private static BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
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
