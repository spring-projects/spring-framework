/*
 * Copyright 2002-2013 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.util.Assert;

/**
 * {@code TestContext} encapsulates the context in which a test is executed,
 * agnostic of the actual testing framework in use.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public class TestContext extends AttributeAccessorSupport {

	private static final long serialVersionUID = -5827157174866681233L;

	private static final Log logger = LogFactory.getLog(TestContext.class);

	private final ContextCache contextCache;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Delegates to {@link #TestContext(Class, ContextCache, String)} with a
	 * value of {@code null} for the default {@code ContextLoader} class name.
	 */
	TestContext(Class<?> testClass, ContextCache contextCache) {
		this(testClass, contextCache, null);
	}

	/**
	 * Construct a new test context for the supplied {@linkplain Class test class}
	 * and {@linkplain ContextCache context cache} and parse the corresponding
	 * {@link ContextConfiguration &#064;ContextConfiguration} or
	 * {@link ContextHierarchy &#064;ContextHierarchy} annotation, if present.
	 * <p>If the supplied class name for the default {@code ContextLoader}
	 * is {@code null} or <em>empty</em> and no concrete {@code ContextLoader}
	 * class is explicitly supplied via {@code @ContextConfiguration}, a
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader
	 * DelegatingSmartContextLoader} or
	 * {@link org.springframework.test.context.web.WebDelegatingSmartContextLoader
	 * WebDelegatingSmartContextLoader} will be used instead.
	 * @param testClass the test class for which the test context should be
	 * constructed (must not be {@code null})
	 * @param contextCache the context cache from which the constructed test
	 * context should retrieve application contexts (must not be
	 * {@code null})
	 * @param defaultContextLoaderClassName the name of the default
	 * {@code ContextLoader} class to use (may be {@code null})
	 */
	TestContext(Class<?> testClass, ContextCache contextCache, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(contextCache, "ContextCache must not be null");

		this.testClass = testClass;
		this.contextCache = contextCache;
		this.cacheAwareContextLoaderDelegate = new CacheAwareContextLoaderDelegate(contextCache);

		MergedContextConfiguration mergedContextConfiguration;

		if (testClass.isAnnotationPresent(ContextConfiguration.class)
				|| testClass.isAnnotationPresent(ContextHierarchy.class)) {
			mergedContextConfiguration = ContextLoaderUtils.buildMergedContextConfiguration(testClass,
				defaultContextLoaderClassName, cacheAwareContextLoaderDelegate);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info(String.format(
					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]",
					testClass.getName()));
			}
			mergedContextConfiguration = new MergedContextConfiguration(testClass, null, null, null, null);
		}

		this.mergedContextConfiguration = mergedContextConfiguration;
	}

	/**
	 * Get the {@link ApplicationContext application context} for this test
	 * context, possibly cached.
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the
	 * application context
	 */
	public ApplicationContext getApplicationContext() {
		return cacheAwareContextLoaderDelegate.loadContext(mergedContextConfiguration);
	}

	/**
	 * Get the {@link Class test class} for this test context.
	 * @return the test class (never {@code null})
	 */
	public Class<?> getTestClass() {
		return testClass;
	}

	/**
	 * Get the current {@link Object test instance} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test instance (may be {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	public Object getTestInstance() {
		return testInstance;
	}

	/**
	 * Get the current {@link Method test method} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test method (may be {@code null})
	 * @see #updateState(Object, Method, Throwable)
	 */
	public Method getTestMethod() {
		return testMethod;
	}

	/**
	 * Get the {@link Throwable exception} that was thrown during execution of
	 * the {@link #getTestMethod() test method}.
	 * <p>Note: this is a mutable property.
	 * @return the exception that was thrown, or {@code null} if no
	 * exception was thrown
	 * @see #updateState(Object, Method, Throwable)
	 */
	public Throwable getTestException() {
		return testException;
	}

	/**
	 * Call this method to signal that the {@linkplain ApplicationContext application
	 * context} associated with this test context is <em>dirty</em> and should be
	 * discarded. Do this if a test has modified the context &mdash; for example,
	 * by replacing a bean definition or modifying the state of a singleton bean.
	 * @deprecated as of Spring 3.2.2; use
	 * {@link #markApplicationContextDirty(DirtiesContext.HierarchyMode)} instead.
	 */
	@Deprecated
	public void markApplicationContextDirty() {
		markApplicationContextDirty((HierarchyMode) null);
	}

	/**
	 * Call this method to signal that the {@linkplain ApplicationContext application
	 * context} associated with this test context is <em>dirty</em> and should be
	 * discarded. Do this if a test has modified the context &mdash; for example,
	 * by replacing a bean definition or modifying the state of a singleton bean.
	 * @param hierarchyMode the context cache clearing mode to be applied if the
	 * context is part of a hierarchy (may be {@code null})
	 */
	public void markApplicationContextDirty(HierarchyMode hierarchyMode) {
		contextCache.remove(mergedContextConfiguration, hierarchyMode);
	}

	/**
	 * Update this test context to reflect the state of the currently executing
	 * test.
	 * @param testInstance the current test instance (may be {@code null})
	 * @param testMethod the current test method (may be {@code null})
	 * @param testException the exception that was thrown in the test method, or
	 * {@code null} if no exception was thrown
	 */
	void updateState(Object testInstance, Method testMethod, Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}

	/**
	 * Provide a String representation of this test context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", testClass)//
		.append("testInstance", testInstance)//
		.append("testMethod", testMethod)//
		.append("testException", testException)//
		.append("mergedContextConfiguration", mergedContextConfiguration)//
		.toString();
	}

}
