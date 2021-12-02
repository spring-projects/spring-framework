/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.style.ToStringCreator;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private final Map<String, Object> attributes = new ConcurrentHashMap<>(4);

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	@Nullable
	private volatile Object testInstance;

	@Nullable
	private volatile Method testMethod;

	@Nullable
	private volatile Throwable testException;


	/**
	 * <em>Copy constructor</em> for creating a new {@code DefaultTestContext}
	 * based on the <em>attributes</em> and immutable state of the supplied context.
	 * <p><em>Immutable state</em> includes all arguments supplied to the
	 * {@linkplain #DefaultTestContext(Class, MergedContextConfiguration,
	 * CacheAwareContextLoaderDelegate) standard constructor}.
	 * @throws NullPointerException if the supplied {@code DefaultTestContext}
	 * is {@code null}
	 */
	public DefaultTestContext(DefaultTestContext testContext) {
		this(testContext.testClass, testContext.mergedContextConfiguration,
			testContext.cacheAwareContextLoaderDelegate);
		this.attributes.putAll(testContext.attributes);
	}

	/**
	 * Construct a new {@code DefaultTestContext} from the supplied arguments.
	 * @param testClass the test class for this test context
	 * @param mergedContextConfiguration the merged application context
	 * configuration for this test context
	 * @param cacheAwareContextLoaderDelegate the delegate to use for loading
	 * and closing the application context for this test context
	 */
	public DefaultTestContext(Class<?> testClass, MergedContextConfiguration mergedContextConfiguration,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		Assert.notNull(testClass, "Test Class must not be null");
		Assert.notNull(mergedContextConfiguration, "MergedContextConfiguration must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * Determine if the {@linkplain ApplicationContext application context} for
	 * this test context is present in the context cache.
	 * @return {@code true} if the application context has already been loaded
	 * and stored in the context cache
	 * @since 5.2
	 * @see #getApplicationContext()
	 * @see CacheAwareContextLoaderDelegate#isContextLoaded
	 */
	@Override
	public boolean hasApplicationContext() {
		return this.cacheAwareContextLoaderDelegate.isContextLoaded(this.mergedContextConfiguration);
	}

	/**
	 * Get the {@linkplain ApplicationContext application context} for this
	 * test context.
	 * <p>The default implementation delegates to the {@link CacheAwareContextLoaderDelegate}
	 * that was supplied when this {@code TestContext} was constructed.
	 * @throws IllegalStateException if the context returned by the context
	 * loader delegate is not <em>active</em> (i.e., has been closed)
	 * @see CacheAwareContextLoaderDelegate#loadContext
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		ApplicationContext context = this.cacheAwareContextLoaderDelegate.loadContext(this.mergedContextConfiguration);
		if (context instanceof ConfigurableApplicationContext cac) {
			Assert.state(cac.isActive(), () ->
					"The ApplicationContext loaded for [" + this.mergedContextConfiguration +
					"] is not active. This may be due to one of the following reasons: " +
					"1) the context was closed programmatically by user code; " +
					"2) the context was closed during parallel test execution either " +
					"according to @DirtiesContext semantics or due to automatic eviction " +
					"from the ContextCache due to a maximum cache size policy.");
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
	@Override
	public void markApplicationContextDirty(@Nullable HierarchyMode hierarchyMode) {
		this.cacheAwareContextLoaderDelegate.closeContext(this.mergedContextConfiguration, hierarchyMode);
	}

	@Override
	public final Class<?> getTestClass() {
		return this.testClass;
	}

	@Override
	public final Object getTestInstance() {
		Object testInstance = this.testInstance;
		Assert.state(testInstance != null, "No test instance");
		return testInstance;
	}

	@Override
	public final Method getTestMethod() {
		Method testMethod = this.testMethod;
		Assert.state(testMethod != null, "No test method");
		return testMethod;
	}

	@Override
	@Nullable
	public final Throwable getTestException() {
		return this.testException;
	}

	@Override
	public void updateState(@Nullable Object testInstance, @Nullable Method testMethod, @Nullable Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.attributes) {
			if (value != null) {
				this.attributes.put(name, value);
			}
			else {
				this.attributes.remove(name);
			}
		}
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		Assert.notNull(name, "Name must not be null");
		return this.attributes.get(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(computeFunction, "Compute function must not be null");
		Object value = this.attributes.computeIfAbsent(name, computeFunction);
		Assert.state(value != null,
				() -> String.format("Compute function must not return null for attribute named '%s'", name));
		return (T) value;
	}

	@Override
	@Nullable
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
		synchronized (this.attributes) {
			return StringUtils.toStringArray(this.attributes.keySet());
		}
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
