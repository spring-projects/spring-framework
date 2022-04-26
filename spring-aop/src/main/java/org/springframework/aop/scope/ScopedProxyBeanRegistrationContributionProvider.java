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

import java.lang.reflect.Executable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generator.CodeContribution;
import org.springframework.aot.generator.DefaultCodeContribution;
import org.springframework.aot.generator.ProtectedAccess.Options;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanInstantiationGenerator;
import org.springframework.beans.factory.generator.BeanRegistrationBeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanRegistrationContributionProvider;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.javapoet.support.MultiStatement;
import org.springframework.lang.Nullable;

/**
 * {@link BeanRegistrationContributionProvider} for {@link ScopedProxyFactoryBean}.
 *
 * @author Stephane Nicoll
 */
class ScopedProxyBeanRegistrationContributionProvider implements BeanRegistrationContributionProvider {

	private static final Log logger = LogFactory.getLog(ScopedProxyBeanRegistrationContributionProvider.class);


	private final ConfigurableBeanFactory beanFactory;

	ScopedProxyBeanRegistrationContributionProvider(ConfigurableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Nullable
	@Override
	public BeanFactoryContribution getContributionFor(String beanName, RootBeanDefinition beanDefinition) {
		Class<?> beanType = beanDefinition.getResolvableType().toClass();
		return (beanType.equals(ScopedProxyFactoryBean.class))
				? createScopedProxyBeanFactoryContribution(beanName, beanDefinition) : null;
	}

	@Nullable
	private BeanFactoryContribution createScopedProxyBeanFactoryContribution(String beanName, RootBeanDefinition beanDefinition) {
		String targetBeanName = getTargetBeanName(beanDefinition);
		BeanDefinition targetBeanDefinition = getTargetBeanDefinition(targetBeanName);
		if (targetBeanDefinition == null) {
			logger.warn("Could not handle " + ScopedProxyFactoryBean.class.getSimpleName() +
					": no target bean definition found with name " + targetBeanName);
			return null;
		}
		RootBeanDefinition processedBeanDefinition = new RootBeanDefinition(beanDefinition);
		processedBeanDefinition.setTargetType(targetBeanDefinition.getResolvableType());
		processedBeanDefinition.getPropertyValues().removePropertyValue("targetBeanName");
		return new BeanRegistrationBeanFactoryContribution(beanName, processedBeanDefinition,
				getBeanInstantiationGenerator(targetBeanName));
	}

	private BeanInstantiationGenerator getBeanInstantiationGenerator(String targetBeanName) {
		return new BeanInstantiationGenerator() {

			@Override
			public Executable getInstanceCreator() {
				return ScopedProxyFactoryBean.class.getDeclaredConstructors()[0];
			}

			@Override
			public CodeContribution generateBeanInstantiation(RuntimeHints runtimeHints) {
				CodeContribution codeContribution = new DefaultCodeContribution(runtimeHints);
				codeContribution.protectedAccess().analyze(getInstanceCreator(), Options.defaults().build());
				MultiStatement statements = new MultiStatement();
				statements.addStatement("$T factory = new $T()", ScopedProxyFactoryBean.class, ScopedProxyFactoryBean.class);
				statements.addStatement("factory.setTargetBeanName($S)", targetBeanName);
				statements.addStatement("factory.setBeanFactory(beanFactory)");
				statements.addStatement("return factory.getObject()");
				codeContribution.statements().add(statements.toLambda("() ->"));
				return codeContribution;
			}
		};
	}

	@Nullable
	private String getTargetBeanName(BeanDefinition beanDefinition) {
		Object value = beanDefinition.getPropertyValues().get("targetBeanName");
		return (value instanceof String targetBeanName) ? targetBeanName : null;
	}

	@Nullable
	private BeanDefinition getTargetBeanDefinition(@Nullable String targetBeanName) {
		if (targetBeanName != null && this.beanFactory.containsBean(targetBeanName)) {
			return this.beanFactory.getMergedBeanDefinition(targetBeanName);
		}
		return null;
	}

}
