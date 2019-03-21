/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * This class is only used within tests in the spring-orm module.
 *
 * <p>Convenient superclass for JUnit 3.8 based tests depending on a Spring
 * context. The test instance itself is populated by Dependency Injection.
 *
 * <p>Supports Setter Dependency Injection: simply express dependencies
 * on objects in the test fixture, and they will be satisfied by autowiring
 * by type.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Rick Evans
 * @author Sam Brannen
 * @since 1.1.1
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
public abstract class AbstractDependencyInjectionSpringContextTests extends AbstractSingleSpringContextTests {

	/**
	 * Constant that indicates no autowiring at all.
	 * @see #setAutowireMode
	 */
	protected static final int AUTOWIRE_NO = AutowireCapableBeanFactory.AUTOWIRE_NO;

	/**
	 * Constant that indicates autowiring bean properties by name.
	 * @see #setAutowireMode
	 */
	protected static final int AUTOWIRE_BY_NAME = AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;

	/**
	 * Constant that indicates autowiring bean properties by type.
	 * @see #setAutowireMode
	 */
	protected static final int AUTOWIRE_BY_TYPE = AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;

	private int autowireMode = AUTOWIRE_BY_TYPE;

	private boolean dependencyCheck = true;


	/**
	 * Set the autowire mode for test properties set by Dependency Injection.
	 * <p>The default is {@link #AUTOWIRE_BY_TYPE}. Can be set to
	 * {@link #AUTOWIRE_BY_NAME} or {@link #AUTOWIRE_NO} instead.
	 * @see #AUTOWIRE_BY_TYPE
	 * @see #AUTOWIRE_BY_NAME
	 * @see #AUTOWIRE_NO
	 */
	protected final void setAutowireMode(final int autowireMode) {
		this.autowireMode = autowireMode;
	}

	/**
	 * Return the autowire mode for test properties set by Dependency Injection.
	 */
	protected final int getAutowireMode() {
		return this.autowireMode;
	}

	/**
	 * Set whether or not dependency checking should be performed for test
	 * properties set by Dependency Injection.
	 * <p>The default is {@code true}, meaning that tests cannot be run
	 * unless all properties are populated.
	 */
	protected final void setDependencyCheck(final boolean dependencyCheck) {
		this.dependencyCheck = dependencyCheck;
	}

	/**
	 * Return whether or not dependency checking should be performed for test
	 * properties set by Dependency Injection.
	 */
	protected final boolean isDependencyCheck() {
		return this.dependencyCheck;
	}

	/**
	 * Prepare this test instance, injecting dependencies into its bean properties.
	 * <p>Note: if the {@link ApplicationContext} for this test instance has not
	 * been configured (e.g., is {@code null}), dependency injection
	 * will naturally <strong>not</strong> be performed, but an informational
	 * message will be written to the log.
	 * @see #injectDependencies()
	 */
	@Override
	protected void prepareTestInstance() throws Exception {
		if (getApplicationContext() == null) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("ApplicationContext has not been configured for test [" + getClass().getName()
						+ "]: dependency injection will NOT be performed.");
			}
		}
		else {
			Assert.state(getApplicationContext() != null,
					"injectDependencies() called without first configuring an ApplicationContext");

			getApplicationContext().getBeanFactory().autowireBeanProperties(this, getAutowireMode(), isDependencyCheck());
		}
	}

}
