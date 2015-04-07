/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TestContext} interface.
 *
 * <p>Although {@code DefaultTestContext} was first introduced in Spring Framework
 * 4.0, the initial implementation of this class was extracted from the existing
 * code base for {@code TestContext} when {@code TestContext} was converted into
 * an interface.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
class DefaultTestContext extends AttributeAccessorSupport implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Construct a new test context using the supplied {@link TestContextBootstrapper}.
	 * @param testContextBootstrapper the {@code TestContextBootstrapper} to use
	 * to construct the test context (must not be {@code null})
	 */
	DefaultTestContext(TestContextBootstrapper testContextBootstrapper) {
		Assert.notNull(testContextBootstrapper, "TestContextBootstrapper must not be null");

		BootstrapContext bootstrapContext = testContextBootstrapper.getBootstrapContext();
		this.testClass = bootstrapContext.getTestClass();
		this.cacheAwareContextLoaderDelegate = bootstrapContext.getCacheAwareContextLoaderDelegate();
		this.mergedContextConfiguration = testContextBootstrapper.buildMergedContextConfiguration();
	}


	public ApplicationContext getApplicationContext() {
		return this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
	}

	public void markApplicationContextDirty(HierarchyMode hierarchyMode) {
		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedContextConfiguration, hierarchyMode);
	}

	public final Class<?> getTestClass() {
		return this.testClass;
	}

	public final Object getTestInstance() {
		return this.testInstance;
	}

	public final Method getTestMethod() {
		return this.testMethod;
	}

	public final Throwable getTestException() {
		return this.testException;
	}

	public void updateState(Object testInstance, Method testMethod, Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}


	/**
	 * Provide a String representation of this test context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("testClass", this.testClass)
				.append("testInstance", this.testInstance)
				.append("testMethod", this.testMethod)
				.append("testException", this.testException)
				.append("mergedContextConfiguration", this.mergedContextConfiguration)
				.toString();
	}

}
