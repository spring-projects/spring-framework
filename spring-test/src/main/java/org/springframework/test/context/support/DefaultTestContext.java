/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.reflect.Method;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link TestContext} interface.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DefaultTestContext extends AttributeAccessorSupport implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Construct a new {@code DefaultTestContext} from the supplied arguments.
	 * @param testClass the test class for this test context; never {@code null}
	 * @param mergedContextConfiguration the merged application context
	 * configuration for this test context; never {@code null}
	 * @param cacheAwareContextLoaderDelegate the delegate to use for loading
	 * and closing the application context for this test context; never {@code null}
	 */
	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedContextConfiguration,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		Assert.notNull(testClass, "testClass must not be null");
		Assert.notNull(mergedContextConfiguration, "MergedContextConfiguration must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * Get the {@linkplain ApplicationContext application context} for this
	 * test context.
	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
	 * that was supplied when this {@code TestContext} was constructed.
	 * @see CacheAwareContextLoaderDelegate#loadContext
	 * @throws IllegalStateException if the context returned by the context
	 * loader delegate is not <em>active</em> (i.e., has been closed).
	 */
	public ApplicationContext getApplicationContext() {
		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		if (context instanceof ConfigurableApplicationContext) {
			@SuppressWarnings("resource")
			ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
			Assert.state(cac.isActive(), "The ApplicationContext loaded for [" + mergedContextConfiguration
					+ "] is not active. Ensure that the context has not been closed programmatically.");
		}
		return context;
	}

	/**
	 * Mark the {@linkplain ApplicationContext application context} associated
	 * with this test context as <em>dirty</em> (i.e., by removing it from the
	 * context cache and closing it).
	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
	 * that was supplied when this {@code TestContext} was constructed.
	 * @see CacheAwareContextLoaderDelegate#closeContext
	 */
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
