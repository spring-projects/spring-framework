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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * TestContext encapsulates the context in which a test is executed, agnostic of
 * the actual testing framework in use.
 * 
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 */
public class TestContext extends AttributeAccessorSupport {

	private static final long serialVersionUID = -5827157174866681233L;

	private static final Log logger = LogFactory.getLog(TestContext.class);

	private final ContextCache contextCache;

	private final ContextLoader contextLoader;

	private final String[] locations;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Delegates to {@link #TestContext(Class, ContextCache, String)} with a
	 * value of <code>null</code> for the default <code>ContextLoader</code>
	 * class name.
	 */
	TestContext(Class<?> testClass, ContextCache contextCache) {
		this(testClass, contextCache, null);
	}

	/**
	 * Construct a new test context for the supplied {@link Class test class}
	 * and {@link ContextCache context cache} and parse the corresponding
	 * {@link ContextConfiguration &#064;ContextConfiguration} annotation, if
	 * present.
	 * <p>If the supplied class name for the default ContextLoader is
	 * <code>null</code> or <em>empty</em> and no <code>ContextLoader</code>
	 * class is explicitly supplied via the
	 * <code>&#064;ContextConfiguration</code> annotation, a
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader
	 * GenericXmlContextLoader} will be used instead.
	 * @param testClass the test class for which the test context should be
	 * constructed (must not be <code>null</code>)
	 * @param contextCache the context cache from which the constructed test
	 * context should retrieve application contexts (must not be
	 * <code>null</code>)
	 * @param defaultContextLoaderClassName the name of the default
	 * <code>ContextLoader</code> class to use (may be <code>null</code>)
	 */
	TestContext(Class<?> testClass, ContextCache contextCache, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(contextCache, "ContextCache must not be null");

		ContextConfiguration contextConfiguration = testClass.getAnnotation(ContextConfiguration.class);
		ContextLoader contextLoader = null;
		String[] locations = null;

		if (contextConfiguration == null) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("@ContextConfiguration not found for class [%s]", testClass));
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ContextConfiguration [%s] for class [%s]", contextConfiguration,
					testClass));
			}
			contextLoader = ContextLoaderUtils.resolveContextLoader(testClass, defaultContextLoaderClassName);
			locations = ContextLoaderUtils.resolveContextLocations(contextLoader, testClass);
		}

		this.testClass = testClass;
		this.contextCache = contextCache;
		this.contextLoader = contextLoader;
		this.locations = locations;
	}

	/**
	 * Load an <code>ApplicationContext</code> for this test context using the
	 * configured <code>ContextLoader</code> and resource locations.
	 * @throws Exception if an error occurs while loading the application context
	 */
	private ApplicationContext loadApplicationContext() throws Exception {
		Assert.notNull(this.contextLoader, "Can not load an ApplicationContext with a NULL 'contextLoader'. "
				+ "Consider annotating your test class with @ContextConfiguration.");
		Assert.notNull(this.locations, "Can not load an ApplicationContext with a NULL 'locations' array. "
				+ "Consider annotating your test class with @ContextConfiguration.");
		return this.contextLoader.loadContext(this.locations);
	}

	/**
	 * Convert the supplied context <code>key</code> to a String representation
	 * for use in caching, logging, etc.
	 */
	private String contextKeyString(Serializable key) {
		return ObjectUtils.nullSafeToString(key);
	}

	/**
	 * Get the {@link ApplicationContext application context} for this test
	 * context, possibly cached.
	 * @return the application context
	 * @throws IllegalStateException if an error occurs while retrieving the
	 * application context
	 */
	public ApplicationContext getApplicationContext() {
		synchronized (this.contextCache) {
			String contextKeyString = contextKeyString(this.locations);
			ApplicationContext context = this.contextCache.get(contextKeyString);
			if (context == null) {
				try {
					context = loadApplicationContext();
					this.contextCache.put(contextKeyString, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
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
		return this.testClass;
	}

	/**
	 * Get the current {@link Object test instance} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test instance (may be <code>null</code>)
	 * @see #updateState(Object,Method,Throwable)
	 */
	public final Object getTestInstance() {
		return this.testInstance;
	}

	/**
	 * Get the current {@link Method test method} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test method (may be <code>null</code>)
	 * @see #updateState(Object, Method, Throwable)
	 */
	public final Method getTestMethod() {
		return this.testMethod;
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
		return this.testException;
	}

	/**
	 * Call this method to signal that the {@link ApplicationContext application
	 * context} associated with this test context is <em>dirty</em> and should
	 * be reloaded. Do this if a test has modified the context (for example, by
	 * replacing a bean definition).
	 */
	public void markApplicationContextDirty() {
		this.contextCache.setDirty(contextKeyString(this.locations));
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
		.append("testClass", this.testClass)//
		.append("locations", this.locations)//
		.append("testInstance", this.testInstance)//
		.append("testMethod", this.testMethod)//
		.append("testException", this.testException)//
		.toString();
	}

}
