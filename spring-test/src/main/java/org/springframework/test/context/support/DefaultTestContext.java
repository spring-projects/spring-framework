/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
 * @author Rob Harrop
 * @since 4.0
 */
public class DefaultTestContext implements TestContext {

	private static final long serialVersionUID = -5827157174866681233L;

	private final Map<String, Object> attributes = new ConcurrentHashMap<>(0);

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private volatile Object testInstance;

	private volatile Method testMethod;

	private volatile Throwable testException;


	/**
	 * <em>Copy constructor</em> for creating a new {@code DefaultTestContext}
	 * based on the immutable state and <em>attributes</em> of the supplied context.
	 *
	 * <p><em>Immutable state</em> includes all arguments supplied to
	 * {@link #DefaultTestContext(Class, MergedContextConfiguration, CacheAwareContextLoaderDelegate)}.
	 */
	public DefaultTestContext(DefaultTestContext testContext) {
		this(testContext.testClass, testContext.mergedContextConfiguration,
			testContext.cacheAwareContextLoaderDelegate);
		testContext.attributes.forEach(this.attributes::put);
	}

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
			Assert.state(cac.isActive(), () -> "The ApplicationContext loaded for [" + mergedContextConfiguration
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

	@Override
	public void setAttribute(String name, Object value) {
		Assert.notNull(name, "Name must not be null");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	public Object removeAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.remove(name);
	}

	@Override
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.containsKey(name);
	}

	@Override
	public String[] attributeNames() {
		return this.attributes.keySet().stream().toArray(String[]::new);
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
				.append("attributes", this.attributes)
				.toString();
	}

}
