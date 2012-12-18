/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.Conventions;
import org.springframework.test.context.TestContext;

/**
 * {@code TestExecutionListener} which provides support for dependency
 * injection and initialization of test instances.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DependencyInjectionTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Attribute name for a {@link TestContext} attribute which indicates
	 * whether or not the dependencies of a test instance should be
	 * <em>reinjected</em> in
	 * {@link #beforeTestMethod(TestContext) beforeTestMethod()}. Note that
	 * dependencies will be injected in
	 * {@link #prepareTestInstance(TestContext) prepareTestInstance()} in any
	 * case.
	 * <p>Clients of a {@link TestContext} (e.g., other
	 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListeners})
	 * may therefore choose to set this attribute to signal that dependencies
	 * should be reinjected <em>between</em> execution of individual test
	 * methods.
	 * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
	 */
	public static final String REINJECT_DEPENDENCIES_ATTRIBUTE = Conventions.getQualifiedAttributeName(
			DependencyInjectionTestExecutionListener.class, "reinjectDependencies");

	private static final Log logger = LogFactory.getLog(DependencyInjectionTestExecutionListener.class);


	/**
	 * Performs dependency injection on the
	 * {@link TestContext#getTestInstance() test instance} of the supplied
	 * {@link TestContext test context} by
	 * {@link AutowireCapableBeanFactory#autowireBeanProperties(Object, int, boolean) autowiring}
	 * and
	 * {@link AutowireCapableBeanFactory#initializeBean(Object, String) initializing}
	 * the test instance via its own
	 * {@link TestContext#getApplicationContext() application context} (without
	 * checking dependencies).
	 * <p>The {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} will be subsequently removed
	 * from the test context, regardless of its value.
	 */
	@Override
	public void prepareTestInstance(final TestContext testContext) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Performing dependency injection for test context [" + testContext + "].");
		}
		injectDependencies(testContext);
	}

	/**
	 * If the {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} in the supplied
	 * {@link TestContext test context} has a value of {@link Boolean#TRUE},
	 * this method will have the same effect as
	 * {@link #prepareTestInstance(TestContext) prepareTestInstance()};
	 * otherwise, this method will have no effect.
	 */
	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		if (Boolean.TRUE.equals(testContext.getAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			if (logger.isDebugEnabled()) {
				logger.debug("Reinjecting dependencies for test context [" + testContext + "].");
			}
			injectDependencies(testContext);
		}
	}

	/**
	 * Performs dependency injection and bean initialization for the supplied
	 * {@link TestContext} as described in
	 * {@link #prepareTestInstance(TestContext) prepareTestInstance()}.
	 * <p>The {@link #REINJECT_DEPENDENCIES_ATTRIBUTE} will be subsequently removed
	 * from the test context, regardless of its value.
	 * @param testContext the test context for which dependency injection should
	 * be performed (never {@code null})
	 * @throws Exception allows any exception to propagate
	 * @see #prepareTestInstance(TestContext)
	 * @see #beforeTestMethod(TestContext)
	 */
	protected void injectDependencies(final TestContext testContext) throws Exception {
		Object bean = testContext.getTestInstance();
		AutowireCapableBeanFactory beanFactory = testContext.getApplicationContext().getAutowireCapableBeanFactory();
		beanFactory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
		beanFactory.initializeBean(bean, testContext.getTestClass().getName());
		testContext.removeAttribute(REINJECT_DEPENDENCIES_ATTRIBUTE);
	}

}
