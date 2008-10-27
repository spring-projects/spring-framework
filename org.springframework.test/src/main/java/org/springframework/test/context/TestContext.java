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

package org.springframework.test.context;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.annotation.AnnotationUtils;
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

	private static final String DEFAULT_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.support.GenericXmlContextLoader";

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
	 * Construct a new test context for the supplied {@link Class test class}
	 * and {@link ContextCache context cache} and parses the corresponding
	 * {@link ContextConfiguration @ContextConfiguration} annotation, if present.
	 * @param testClass the {@link Class} object corresponding to the test class
	 * for which the test context should be constructed (must not be <code>null</code>)
	 * @param contextCache the context cache from which the constructed test context
	 * should retrieve application contexts (must not be <code>null</code>)
	 */
	@SuppressWarnings("unchecked")
	TestContext(Class<?> testClass, ContextCache contextCache) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(contextCache, "ContextCache must not be null");

		ContextConfiguration contextConfiguration = testClass.getAnnotation(ContextConfiguration.class);
		String[] locations = null;
		ContextLoader contextLoader = null;

		if (contextConfiguration == null) {
			if (logger.isInfoEnabled()) {
				logger.info("@ContextConfiguration not found for class [" + testClass + "]");
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved @ContextConfiguration [" + contextConfiguration + "] for class [" + testClass + "]");
			}

			Class<? extends ContextLoader> contextLoaderClass = contextConfiguration.loader();
			if (ContextLoader.class.equals(contextLoaderClass)) {
				try {
					contextLoaderClass = (Class<? extends ContextLoader>) getClass().getClassLoader().loadClass(
							DEFAULT_CONTEXT_LOADER_CLASS_NAME);
				}
				catch (ClassNotFoundException ex) {
					throw new IllegalStateException("Could not load default ContextLoader class ["
							+ DEFAULT_CONTEXT_LOADER_CLASS_NAME + "]. Specify @ContextConfiguration's 'loader' "
							+ "attribute or make the default loader class available.");
				}
			}
			contextLoader = (ContextLoader) BeanUtils.instantiateClass(contextLoaderClass);
			locations = retrieveContextLocations(contextLoader, testClass);
		}

		this.testClass = testClass;
		this.contextCache = contextCache;
		this.contextLoader = contextLoader;
		this.locations = locations;
	}

	/**
	 * Retrieve {@link ApplicationContext} resource locations for the supplied
	 * {@link Class class}, using the supplied {@link ContextLoader} to
	 * {@link ContextLoader#processLocations(Class, String...) process} the
	 * locations.
	 * <p>Note that the
	 * {@link ContextConfiguration#inheritLocations() inheritLocations} flag of
	 * {@link ContextConfiguration @ContextConfiguration} will be taken into
	 * consideration. Specifically, if the <code>inheritLocations</code> flag
	 * is set to <code>true</code>, locations defined in the annotated class
	 * will be appended to the locations defined in superclasses.
	 * @param contextLoader the ContextLoader to use for processing the locations
	 * (must not be <code>null</code>)
	 * @param clazz the class for which to retrieve the resource locations
	 * (must not be <code>null</code>)
	 * @return the list of ApplicationContext resource locations for the specified
	 * class, including locations from superclasses if appropriate
	 * @throws IllegalArgumentException if {@link ContextConfiguration @ContextConfiguration}
	 * is not <em>present</em> on the supplied class
	 */
	private String[] retrieveContextLocations(ContextLoader contextLoader, Class<?> clazz) {
		Assert.notNull(contextLoader, "ContextLoader must not be null");
		Assert.notNull(clazz, "Class must not be null");

		List<String> locationsList = new ArrayList<String>();
		Class<ContextConfiguration> annotationType = ContextConfiguration.class;
		Class<?> declaringClass = AnnotationUtils.findAnnotationDeclaringClass(annotationType, clazz);
		Assert.notNull(declaringClass, "Could not find an 'annotation declaring class' for annotation type [" +
				annotationType + "] and class [" + clazz + "]");

		while (declaringClass != null) {
			ContextConfiguration contextConfiguration = declaringClass.getAnnotation(annotationType);
			if (logger.isTraceEnabled()) {
				logger.trace("Retrieved @ContextConfiguration [" + contextConfiguration + "] for declaring class ["
						+ declaringClass + "]");
			}
			String[] locations = contextLoader.processLocations(declaringClass, contextConfiguration.locations());
			locationsList.addAll(0, Arrays.<String> asList(locations));
			declaringClass = contextConfiguration.inheritLocations() ? AnnotationUtils.findAnnotationDeclaringClass(
					annotationType, declaringClass.getSuperclass()) : null;
		}

		return locationsList.toArray(new String[locationsList.size()]);
	}

	/**
	 * Build an {@link ApplicationContext} for this test context using the
	 * configured {@link #getContextLoader() ContextLoader} and
	 * {@link #getLocations() resource locations}.
	 * @throws Exception if an error occurs while building the application context
	 */
	private ApplicationContext loadApplicationContext() throws Exception {
		Assert.notNull(getContextLoader(),
				"Can not build an ApplicationContext with a NULL 'contextLoader'. Consider annotating your test class with @ContextConfiguration.");
		Assert.notNull(getLocations(),
				"Can not build an ApplicationContext with a NULL 'locations' array. Consider annotating your test class with @ContextConfiguration.");
		return getContextLoader().loadContext(getLocations());
	}

	/**
	 * Convert the supplied context <code>key</code> to a String
	 * representation for use in caching, logging, etc.
	 * @param key the context key to convert to a String
	 */
	private String contextKeyString(Serializable key) {
		return ObjectUtils.nullSafeToString(key);
	}

	/**
	 * Get the {@link ApplicationContext application context} for this test
	 * context, possibly cached.
	 * @return the application context; may be <code>null</code> if the
	 * current test context is not configured to use an application context
	 * @throws IllegalStateException if an error occurs while retrieving the application context
	 */
	public ApplicationContext getApplicationContext() {
		ApplicationContext context = null;
		ContextCache cache = getContextCache();
		synchronized (cache) {
			context = cache.get(contextKeyString(getLocations()));
			if (context == null) {
				try {
					context = loadApplicationContext();
					cache.put(contextKeyString(getLocations()), context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
		}
		return context;
	}

	/**
	 * Get the {@link ContextCache context cache} for this test context.
	 * @return the context cache (never <code>null</code>)
	 */
	ContextCache getContextCache() {
		return this.contextCache;
	}

	/**
	 * Get the {@link ContextLoader} to use for loading the
	 * {@link ApplicationContext} for this test context.
	 * @return the context loader. May be <code>null</code> if the current
	 * test context is not configured to use an application context.
	 */
	ContextLoader getContextLoader() {
		return this.contextLoader;
	}

	/**
	 * Get the resource locations to use for loading the
	 * {@link ApplicationContext} for this test context.
	 * @return the application context resource locations.
	 * May be <code>null</code> if the current test context is
	 * not configured to use an application context.
	 */
	String[] getLocations() {
		return this.locations;
	}

	/**
	 * Get the {@link Class test class} for this test context.
	 * @return the test class (never <code>null</code>)
	 */
	public final Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * Gets the current {@link Object test instance} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test instance (may be <code>null</code>)
	 * @see #updateState(Object,Method,Throwable)
	 */
	public final Object getTestInstance() {
		return this.testInstance;
	}

	/**
	 * Gets the current {@link Method test method} for this test context.
	 * <p>Note: this is a mutable property.
	 * @return the current test method (may be <code>null</code>)
	 * @see #updateState(Object, Method, Throwable)
	 */
	public final Method getTestMethod() {
		return this.testMethod;
	}

	/**
	 * Gets the {@link Throwable exception} that was thrown during execution of
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
	 * Call this method to signal that the
	 * {@link ApplicationContext application context} associated with this test
	 * context is <em>dirty</em> and should be reloaded. Do this if a test has
	 * modified the context (for example, by replacing a bean definition).
	 */
	public void markApplicationContextDirty() {
		getContextCache().setDirty(contextKeyString(getLocations()));
	}

	/**
	 * Updates this test context to reflect the state of the currently executing test.
	 * @param testInstance the current test instance (may be <code>null</code>)
	 * @param testMethod the current test method (may be <code>null</code>)
	 * @param testException the exception that was thrown in the test method,
	 * or <code>null</code> if no exception was thrown
	 */
	synchronized void updateState(Object testInstance, Method testMethod, Throwable testException) {
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testException = testException;
	}

	/**
	 * Provides a string representation of this test context's
	 * {@link #getTestClass() test class},
	 * {@link #getLocations() application context resource locations},
	 * {@link #getTestInstance() test instance},
	 * {@link #getTestMethod() test method}, and
	 * {@link #getTestException() test exception}.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this).
				append("testClass", getTestClass()).
				append("locations", getLocations()).append("testInstance", getTestInstance()).
				append("testMethod", getTestMethod()).append("testException", getTestException()).
				toString();
	}

}
