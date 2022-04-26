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

package org.springframework.aop.scope;

import org.junit.jupiter.api.Test;

import org.springframework.aop.testfixture.scope.SimpleTarget;
import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import org.springframework.core.ResolvableType;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.support.CodeSnippet;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScopedProxyBeanRegistrationContributionProvider}.
 *
 * @author Stephane Nicoll
 */
class ScopedProxyBeanRegistrationContributionProviderTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	@Test
	void getWithNonScopedProxy() {
		BeanDefinition beanDefinition = BeanDefinitionBuilder.rootBeanDefinition(PropertiesFactoryBean.class)
				.getBeanDefinition();
		assertThat(getBeanFactoryContribution("test", beanDefinition)).isNull();
	}

	@Test
	void getWithScopedProxyWithoutTargetBeanName() {
		BeanDefinition scopeBean = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.getBeanDefinition();
		assertThat(getBeanFactoryContribution("test", scopeBean)).isNull();
	}

	@Test
	void getWithScopedProxyWithInvalidTargetBeanName() {
		BeanDefinition scopeBean = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "testDoesNotExist").getBeanDefinition();
		assertThat(getBeanFactoryContribution("test", scopeBean)).isNull();
	}

	@Test
	void getWithScopedProxyWithTargetBeanName() {
		BeanDefinition targetBean = BeanDefinitionBuilder.rootBeanDefinition(SimpleTarget.class)
				.getBeanDefinition();
		beanFactory.registerBeanDefinition("simpleTarget", targetBean);
		BeanDefinition scopeBean = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "simpleTarget").getBeanDefinition();
		assertThat(getBeanFactoryContribution("test", scopeBean)).isNotNull();
	}

	@Test
	void writeBeanRegistrationForScopedProxy() {
		RootBeanDefinition targetBean = new RootBeanDefinition();
		targetBean.setTargetType(ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		targetBean.setScope("custom");
		this.beanFactory.registerBeanDefinition("numberHolder", targetBean);
		BeanDefinition scopeBean = BeanDefinitionBuilder.rootBeanDefinition(ScopedProxyFactoryBean.class)
				.addPropertyValue("targetBeanName", "numberHolder").getBeanDefinition();
		assertThat(writeBeanRegistration("test", scopeBean).getSnippet()).isEqualTo("""
				BeanDefinitionRegistrar.of("test", ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class))
						.instanceSupplier(() -> {
							ScopedProxyFactoryBean factory = new ScopedProxyFactoryBean();
							factory.setTargetBeanName("numberHolder");
							factory.setBeanFactory(beanFactory);
							return factory.getObject();
						}).register(beanFactory);
				""");
	}

	private CodeSnippet writeBeanRegistration(String beanName, BeanDefinition beanDefinition) {
		BeanFactoryContribution contribution = getBeanFactoryContribution(beanName, beanDefinition);
		assertThat(contribution).isNotNull();
		BeanFactoryInitialization initialization = new BeanFactoryInitialization(new DefaultGeneratedTypeContext("comp.example", packageName -> GeneratedType.of(ClassName.get(packageName, "Test"))));
		contribution.applyTo(initialization);
		return CodeSnippet.of(initialization.toCodeBlock());
	}

	@Nullable
	BeanFactoryContribution getBeanFactoryContribution(String beanName, BeanDefinition beanDefinition) {
		ScopedProxyBeanRegistrationContributionProvider provider = new ScopedProxyBeanRegistrationContributionProvider(this.beanFactory);
		return provider.getContributionFor(beanName, (RootBeanDefinition) beanDefinition);
	}

}

