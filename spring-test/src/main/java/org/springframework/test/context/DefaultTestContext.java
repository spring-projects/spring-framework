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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.core.style.ToStringCreator;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.util.Assert;

import static org.springframework.core.annotation.AnnotationUtils.*;

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

	private static final Log logger = LogFactory.getLog(DefaultTestContext.class);

	private final ContextCache contextCache;

	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;

	private final MergedContextConfiguration mergedContextConfiguration;

	private final Class<?> testClass;

	private Object testInstance;

	private Method testMethod;

	private Throwable testException;


	/**
	 * Delegates to {@link #DefaultTestContext(Class, ContextCache, String)} with a
	 * value of {@code null} for the default {@code ContextLoader} class name.
	 */
	DefaultTestContext(Class<?> testClass, ContextCache contextCache) {
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
	DefaultTestContext(Class<?> testClass, ContextCache contextCache, String defaultContextLoaderClassName) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(contextCache, "ContextCache must not be null");

		this.testClass = testClass;
		this.contextCache = contextCache;
		this.cacheAwareContextLoaderDelegate = new CacheAwareContextLoaderDelegate(contextCache);

		MergedContextConfiguration mergedContextConfiguration;

		final Class<ContextConfiguration> contextConfigType = ContextConfiguration.class;
		final Class<ContextHierarchy> contextHierarchyType = ContextHierarchy.class;
		final List<Class<? extends Annotation>> annotationTypes = Arrays.asList(contextConfigType, contextHierarchyType);

		// TODO Switch to MetaAnnotationUtils for proper meta-annotation support
		if (findAnnotationDeclaringClassForTypes(annotationTypes, testClass) != null) {
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
	 * {@inheritDoc}
	 */
	public ApplicationContext getApplicationContext() {
		return cacheAwareContextLoaderDelegate.loadContext(mergedContextConfiguration);
	}

	/**
	 * {@inheritDoc}
	 */
	public final Class<?> getTestClass() {
		return testClass;
	}

	/**
	 * {@inheritDoc}
	 */
	public final Object getTestInstance() {
		return testInstance;
	}

	/**
	 * {@inheritDoc}
	 */
	public final Method getTestMethod() {
		return testMethod;
	}

	/**
	 * {@inheritDoc}
	 */
	public final Throwable getTestException() {
		return testException;
	}

	/**
	 * {@inheritDoc}
	 */
	public void markApplicationContextDirty(HierarchyMode hierarchyMode) {
		contextCache.remove(mergedContextConfiguration, hierarchyMode);
	}

	/**
	 * {@inheritDoc}
	 */
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
		return new ToStringCreator(this)//
		.append("testClass", testClass)//
		.append("testInstance", testInstance)//
		.append("testMethod", testMethod)//
		.append("testException", testException)//
		.append("mergedContextConfiguration", mergedContextConfiguration)//
		.toString();
	}

}
