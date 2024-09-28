/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NativeDetector;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that resets any mock beans that have been marked
 * with a {@link MockReset}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoTestExecutionListener
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
 */
public class MockitoResetTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Executes before {@link org.springframework.test.context.bean.override.BeanOverrideTestExecutionListener}.
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (MockitoTestExecutionListener.mockitoPresent && !NativeDetector.inNativeImage()) {
			resetMocks(testContext.getApplicationContext(), MockReset.BEFORE);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (MockitoTestExecutionListener.mockitoPresent && !NativeDetector.inNativeImage()) {
			resetMocks(testContext.getApplicationContext(), MockReset.AFTER);
		}
	}

	private void resetMocks(ApplicationContext applicationContext, MockReset reset) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			resetMocks(configurableContext, reset);
		}
	}

	private void resetMocks(ConfigurableApplicationContext applicationContext, MockReset reset) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		Set<String> instantiatedSingletons = new HashSet<>(Arrays.asList(beanFactory.getSingletonNames()));
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			if (beanDefinition.isSingleton() && instantiatedSingletons.contains(beanName)) {
				Object bean = getBean(beanFactory, beanName);
				if (bean != null && reset == MockReset.get(bean)) {
					Mockito.reset(bean);
				}
			}
		}
		try {
			beanFactory.getBean(MockitoBeans.class).resetAll();
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		if (applicationContext.getParent() != null) {
			resetMocks(applicationContext.getParent(), reset);
		}
	}

	@Nullable
	private Object getBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
		try {
			if (isStandardBeanOrSingletonFactoryBean(beanFactory, beanName)) {
				return beanFactory.getBean(beanName);
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return beanFactory.getSingleton(beanName);
	}

	private boolean isStandardBeanOrSingletonFactoryBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
		String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		if (beanFactory.containsBean(factoryBeanName)) {
			FactoryBean<?> factoryBean = (FactoryBean<?>) beanFactory.getBean(factoryBeanName);
			return factoryBean.isSingleton();
		}
		return true;
	}

}
