/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeContext;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.support.CodeSnippet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BeanDefinitionsContribution}.
 *
 * @author Stephane Nicoll
 */
class BeanDefinitionsContributionTests {

	@Test
	void contributeThrowsContributionNotFoundIfNoContributionIsAvailable() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition());
		BeanDefinitionsContribution contribution = new BeanDefinitionsContribution(beanFactory,
				List.of(Mockito.mock(BeanRegistrationContributionProvider.class)));
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(createGenerationContext());
		assertThatThrownBy(() -> contribution.applyTo(initialization))
				.isInstanceOfSatisfying(BeanRegistrationContributionNotFoundException.class, ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getBeanDefinition()).isSameAs(beanFactory.getMergedBeanDefinition("test"));
				});
	}

	@Test
	void contributeThrowsBeanRegistrationExceptionIfContributionThrowsException() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition());
		BeanFactoryContribution testContribution = Mockito.mock(BeanFactoryContribution.class);
		IllegalStateException testException = new IllegalStateException();
		BDDMockito.willThrow(testException).given(testContribution).applyTo(ArgumentMatchers.any(BeanFactoryInitialization.class));
		BeanDefinitionsContribution contribution = new BeanDefinitionsContribution(beanFactory,
				List.of(new TestBeanRegistrationContributionProvider("test", testContribution)));
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(createGenerationContext());
		assertThatThrownBy(() -> contribution.applyTo(initialization))
				.isInstanceOfSatisfying(BeanDefinitionGenerationException.class, ex -> {
					assertThat(ex.getBeanName()).isEqualTo("test");
					assertThat(ex.getBeanDefinition()).isSameAs(beanFactory.getMergedBeanDefinition("test"));
					assertThat(ex.getCause()).isEqualTo(testException);
				});
	}

	@Test
	void contributeGeneratesBeanDefinitionsInOrder() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("counter", BeanDefinitionBuilder
				.rootBeanDefinition(Integer.class, "valueOf").addConstructorArgValue(42).getBeanDefinition());
		beanFactory.registerBeanDefinition("name", BeanDefinitionBuilder
				.rootBeanDefinition(String.class).addConstructorArgValue("Hello").getBeanDefinition());
		CodeSnippet code = contribute(beanFactory, createGenerationContext());
		assertThat(code.getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("counter", Integer.class).withFactoryMethod(Integer.class, "valueOf", int.class)
						.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> Integer.valueOf(attributes.get(0)))).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, 42)).register(beanFactory);
				BeanDefinitionRegistrar.of("name", String.class).withConstructor(String.class)
						.instanceSupplier((instanceContext) -> instanceContext.create(beanFactory, (attributes) -> new String(attributes.get(0, String.class)))).customize((bd) -> bd.getConstructorArgumentValues().addIndexedArgumentValue(0, "Hello")).register(beanFactory);
				""");
	}

	private CodeSnippet contribute(DefaultListableBeanFactory beanFactory, GeneratedTypeContext generationContext) {
		BeanDefinitionsContribution contribution = new BeanDefinitionsContribution(beanFactory);
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(generationContext);
		contribution.applyTo(initialization);
		return CodeSnippet.of(initialization.toCodeBlock());
	}

	private GeneratedTypeContext createGenerationContext() {
		return new DefaultGeneratedTypeContext("com.example", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Test")));
	}

	static class TestBeanRegistrationContributionProvider implements BeanRegistrationContributionProvider {

		private final String beanName;

		private final BeanFactoryContribution contribution;

		public TestBeanRegistrationContributionProvider(String beanName, BeanFactoryContribution contribution) {
			this.beanName = beanName;
			this.contribution = contribution;
		}

		@Override
		public BeanFactoryContribution getContributionFor(String beanName, RootBeanDefinition beanDefinition) {
			return (beanName.equals(this.beanName) ? this.contribution : null);
		}
	}

}
