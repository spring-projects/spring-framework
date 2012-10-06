/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.util.Assert;

/**
 * <code>TestContext</code> encapsulates the context in which a test is executed,
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

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Delegates to {@link #TestContext(Class, ContextCache, String)} with a
	 * value of <code>null</code> for the default {@code ContextLoader} class name.
	 */
	TestContext(Class<?> testClass, ContextCache contextCache) {
		this(testClass, contextCache, null);
	}

	/**
	 * Construct a new test context for the supplied {@link Class test class}
	 * and {@link ContextCache context cache} and parse the corresponding
	 * {@link ContextConfiguration &#064;ContextConfiguration} annotation, if
	 * present.
	 * <p>If the supplied class name for the default {@code ContextLoader}
	 * is <code>null</code> or <em>empty</em> and no concrete {@code ContextLoader}
	 * class is explicitly supplied via the {@code @ContextConfiguration}
	 * annotation, a
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader
	 * DelegatingSmartContextLoader} will be used instead.
	 * @param testClass the test class for which the test context should be
	 * constructed (must not be <code>null</code>)
	 * @param contextCache the context cache from which the constructed test
	 * context should retrieve application contexts (must not be
	 * <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * {@code ContextLoader} class to use (may be <code>null</code>)
	 */
	TestContext(Class<?> testClass, ContextCache contextCache, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(contextCache, "ContextCache must not be null");

		MergedContextConfiguration mergedContextConfiguration;
		ContextConfiguration contextConfiguration = testClass.getAnnotation(ContextConfiguration.class);

		if (contextConfiguration == null) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("@ContextConfiguration not found for class [%s]", testClass));
			}
			mergedContextConfiguration = new MergedContextConfiguration(testClass, null, null, null, null);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ContextConfiguration [%s] for class [%s]", contextConfiguration,
					testClass));
			}
			mergedContextConfiguration = ContextLoaderUtils.buildMergedContextConfiguration(testClass,
				defaultContextLoaderClassName);
		}

		this.contextCache = contextCache;
		this.mergedContextConfiguration = mergedContextConfiguration;
		this.testClass = testClass;
	}

	/**
	 * Load an <code>ApplicationContext</code> for this test context using the
	 * configured {@code ContextLoader} and merged context configuration. Supports
	 * both the {@link SmartContextLoader} and {@link ContextLoader} SPIs.
	 * @throws Exception if an error occurs while loading the application context
	 */
	private ApplicationContext loadApplicationContext() throws Exception {
		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. "
				+ "Consider annotating your test class with @ContextConfiguration.");

		ApplicationContext applicationContext;

		if (contextLoader instanceof SmartContextLoader) {
			SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
			applicationContext = smartContextLoader.loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. "
					+ "Consider annotating your test class with @ContextConfiguration.");
			applicationContext = contextLoader.loadContext(locations);
		}

		return applicationContext;
	}

	/**
	 * Get the {@link ApplicationContext application context} for this test
	 * context, possibly cached.
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the
	 * application context
	 */
	public ApplicationContext getApplicationContext() {
		synchronized (contextCache) {
			ApplicationContext context = contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadApplicationContext();
					if (logger.isDebugEnabled()) {
						logger.debug(String.format(
							"Storing ApplicationContext for test class [%s] in cache under key [%s].", testClass,
							mergedContextConfiguration));
					}
					contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Retrieved ApplicationContext for test class [%s] from cache with key [%s].", testClass,
						mergedContextConfiguration));
				}
			}
			return context;
		}
	}

	/**
	 * Get the {@link Class test class} for this test context.
	 * @return the test class (never <code>null</code>)
	 */
	public final Class<?> getTestClass() {
		return testClass;
	}

	/**
	 * Get the current {@link Object test instance} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test instance (may be <code>null</code>)
	 * @see #updateState(Object,Method,Throwable)
	 */
	public final Object getTestInstance() {
		return testInstance;
	}

	/**
	 * Get the current {@link Method test method} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test method (may be <code>null</code>)
	 * @see #updateState(Object, Method, Throwable)
	 */
	public final Method getTestMethod() {
		return testMethod;
	}

	/**
	 * Get the {@link Throwable exception} that was thrown during execution of
	 * the {@link #getTestMethod() test method}.
	 * <p>Note: this is a mutable property.
	 * @return the exception that was thrown, or <code>null</code> if no
	 * exception was thrown
	 * @see #updateState(Object, Method, Throwable)
	 */
	public final Throwable getTestException() {
		return testException;
	}

	/**
	 * Call this method to signal that the {@link ApplicationContext application
	 * context} associated with this test context is <em>dirty</em> and should
	 * be reloaded. Do this if a test has modified the context (for example, by
	 * replacing a bean definition).
	 */
	public void markApplicationContextDirty() {
		synchronized (contextCache) {
			contextCache.setDirty(mergedContextConfiguration);
		}
	}

	/**
	 * Update this test context to reflect the state of the currently executing
	 * test.
	 * @param testInstance the current test instance (may be <code>null</code>)
	 * @param testMethod the current test method (may be <code>null</code>)
	 * @param testException the exception that was thrown in the test method, or
	 * <code>null</code> if no exception was thrown
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
