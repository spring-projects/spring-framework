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

package org.springframework.transaction.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
class TransactionBeanRegistrationAotProcessorTests {

	private final TransactionBeanRegistrationAotProcessor processor = new TransactionBeanRegistrationAotProcessor();

	private final GenerationContext generationContext = new TestGenerationContext();

	@Test
	void shouldSkipNonAnnotatedType() {
		process(NonAnnotatedBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
	}

	@Test
	void shouldSkipAnnotatedTypeWithNoInterface() {
		process(NoInterfaceBean.class);
		assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
	}

	@Test
	void shouldProcessTransactionalOnClass() {
		process(TransactionalOnTypeBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessJakartaTransactionalOnClass() {
		process(JakartaTransactionalOnTypeBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessTransactionalOnInterface() {
		process(TransactionalOnTypeInterface.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(TransactionalOnTypeInterface.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessTransactionalOnClassMethod() {
		process(TransactionalOnClassMethodBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
	}

	@Test
	void shouldProcessTransactionalOnInterfaceMethod() {
		process(TransactionalOnInterfaceMethodBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(TransactionalOnMethodInterface.class)
				.withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
	}

	private void process(Class<?> beanClass) {
		BeanRegistrationAotContribution contribution = createContribution(beanClass);
		if (contribution != null) {
			contribution.applyTo(this.generationContext, mock());
		}
	}

	@Nullable
	private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
		return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
	}


	@SuppressWarnings("unused")
	static class NonAnnotatedBean {

		public void notTransactional() {
		}
	}

	@SuppressWarnings("unused")
	@Transactional
	static class NoInterfaceBean {

		public void notTransactional() {
		}
	}

	@Transactional
	static class TransactionalOnTypeBean implements NonAnnotatedTransactionalInterface {

		@Override
		public void transactional() {
		}
	}

	@jakarta.transaction.Transactional
	static class JakartaTransactionalOnTypeBean implements NonAnnotatedTransactionalInterface {

		@Override
		public void transactional() {
		}
	}

	interface NonAnnotatedTransactionalInterface {

		void transactional();
	}

	@Transactional
	interface TransactionalOnTypeInterface {

		void transactional();
	}

	static class TransactionalOnClassMethodBean implements NonAnnotatedTransactionalInterface {

		@Override
		@Transactional
		public void transactional() {
		}
	}

	interface TransactionalOnMethodInterface {

		@Transactional
		void transactional();
	}

	static class TransactionalOnInterfaceMethodBean implements TransactionalOnMethodInterface {

		@Override
		public void transactional() {
		}
	}
}
