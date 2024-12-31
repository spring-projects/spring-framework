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

package org.springframework.test.context.bean.override.mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * {@code TestExecutionListener} that resets any mock beans that have been marked
 * with a {@link MockReset}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
 */
public class MockitoResetTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(MockitoResetTestExecutionListener.class);

	/**
	 * Boolean flag which tracks whether Mockito is present in the classpath.
	 * @see #mockitoInitialized
	 * @see #isEnabled()
	 */
	private static final boolean mockitoPresent = ClassUtils.isPresent("org.mockito.Mockito",
			MockitoResetTestExecutionListener.class.getClassLoader());

	/**
	 * Boolean flag which tracks whether Mockito has been successfully initialized
	 * in the current environment.
	 * <p>Even if {@link #mockitoPresent} evaluates to {@code true}, this flag
	 * may eventually evaluate to {@code false} &mdash; for example, in a GraalVM
	 * native image if the necessary reachability metadata has not been registered
	 * for the {@link org.mockito.plugins.MockMaker} in use.
	 * @see #mockitoPresent
	 * @see #isEnabled()
	 */
	private static volatile @Nullable Boolean mockitoInitialized;


	/**
	 * Returns {@code Ordered.LOWEST_PRECEDENCE - 100}.
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) {
		if (isEnabled()) {
			resetMocks(testContext.getApplicationContext(), MockReset.BEFORE);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		if (isEnabled()) {
			resetMocks(testContext.getApplicationContext(), MockReset.AFTER);
		}
	}


	private static void resetMocks(ApplicationContext applicationContext, MockReset reset) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			resetMocks(configurableContext, reset);
		}
	}

	private static void resetMocks(ConfigurableApplicationContext applicationContext, MockReset reset) {
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
			beanFactory.getBean(MockitoBeans.class).resetAll(reset);
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		if (applicationContext.getParent() != null) {
			resetMocks(applicationContext.getParent(), reset);
		}
	}

	private static @Nullable Object getBean(ConfigurableListableBeanFactory beanFactory, String beanName) {
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

	private static boolean isStandardBeanOrSingletonFactoryBean(BeanFactory beanFactory, String beanName) {
		String factoryBeanName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		if (beanFactory.containsBean(factoryBeanName)) {
			FactoryBean<?> factoryBean = (FactoryBean<?>) beanFactory.getBean(factoryBeanName);
			return factoryBean.isSingleton();
		}
		return true;
	}

	/**
	 * Determine if this listener is enabled in the current environment.
	 * @see #mockitoPresent
	 * @see #mockitoInitialized
	 */
	private static boolean isEnabled() {
		if (!mockitoPresent) {
			return false;
		}
		Boolean enabled = mockitoInitialized;
		if (enabled == null) {
			try {
				// Invoke isMock() on a non-null object to initialize core Mockito classes
				// in order to reliably determine if this listener is "enabled" both on the
				// JVM as well as within a GraalVM native image.
				Mockito.mockingDetails("a string is not a mock").isMock();

				// If we got this far, we assume Mockito is usable in the current environment.
				enabled = true;
			}
			catch (Throwable ex) {
				enabled = false;
				if (logger.isDebugEnabled()) {
					logger.debug("""
							MockitoResetTestExecutionListener is disabled in the current environment. \
							See exception for details.""", ex);
				}
			}
			mockitoInitialized = enabled;
		}
		return enabled;
	}

}
