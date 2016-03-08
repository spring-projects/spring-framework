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

package org.springframework.test.context;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

import static org.springframework.beans.BeanUtils.instantiateClass;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
abstract class BootstrapUtils {

	private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME = "org.springframework.test.context.support.DefaultBootstrapContext";

	private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME = "org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME = "org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME = "org.springframework.test.context.web.WebTestContextBootstrapper";

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME = "org.springframework.test.context.web.WebAppConfiguration";

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	private BootstrapUtils() {
		/* no-op */
	}

	/**
	 * Create the {@code BootstrapContext} for the specified {@linkplain Class test class}.
	 *
	 * <p>Uses reflection to create a {@link org.springframework.test.context.support.DefaultBootstrapContext}
	 * that uses a {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}.
	 *
	 * @param testClass the test class for which the bootstrap context should be created
	 * @return a new {@code BootstrapContext}; never {@code null}
	 */
	@SuppressWarnings("unchecked")
	static BootstrapContext createBootstrapContext(Class<?> testClass) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();

		Class<? extends BootstrapContext> clazz = null;
		try {
			clazz = (Class<? extends BootstrapContext>) ClassUtils.forName(DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME,
				BootstrapUtils.class.getClassLoader());

			Constructor<? extends BootstrapContext> constructor = clazz.getConstructor(Class.class,
				CacheAwareContextLoaderDelegate.class);

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating BootstrapContext using constructor [%s]", constructor));
			}
			return instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Could not load BootstrapContext [" + clazz + "]", t);
		}
	}

	@SuppressWarnings("unchecked")
	private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
		Class<? extends CacheAwareContextLoaderDelegate> clazz = null;
		try {
			clazz = (Class<? extends CacheAwareContextLoaderDelegate>) ClassUtils.forName(
				DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME, BootstrapUtils.class.getClassLoader());

			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]",
					clazz.getName()));
			}
			return instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
		}
		catch (Throwable t) {
			throw new IllegalStateException("Could not load CacheAwareContextLoaderDelegate [" + clazz + "]", t);
		}
	}

	/**
	 * Resolve the {@link TestContextBootstrapper} type for the test class in the
	 * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
	 * to the {@link BootstrapContext}.
	 *
	 * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
	 * the test class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type.
	 * Otherwise, either the
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} or the
	 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
	 * WebTestContextBootstrapper} will be used, depending on the presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}.
	 *
	 * @param bootstrapContext the bootstrap context to use
	 * @return a fully configured {@code TestContextBootstrapper}
	 */
	static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		Class<?> testClass = bootstrapContext.getTestClass();

		Class<?> clazz = null;
		try {
			clazz = resolveExplicitTestContextBootstrapper(testClass);
			if (clazz == null) {
				clazz = resolveDefaultTestContextBootstrapper(testClass);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
					testClass.getName(), clazz.getName()));
			}

			TestContextBootstrapper testContextBootstrapper = instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);

			return testContextBootstrapper;
		}
		catch (Throwable ex) {
			if (ex instanceof IllegalStateException) {
				throw (IllegalStateException) ex;
			}
			throw new IllegalStateException("Could not load TestContextBootstrapper [" + clazz
					+ "]. Specify @BootstrapWith's 'value' attribute "
					+ "or make the default bootstrapper class available.", ex);
		}
	}

	/**
	 * @since 4.3
	 */
	private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
		MultiValueMap<String, Object> attributesMultiMap = AnnotatedElementUtils.getAllAnnotationAttributes(
				testClass, BootstrapWith.class.getName());
		List<Object> values = (attributesMultiMap == null ? null : attributesMultiMap.get(AnnotationUtils.VALUE));
		if (values == null) {
			return null;
		}
		Assert.state(values.size() == 1, String.format("Configuration error: found multiple declarations of "
				+ "@BootstrapWith on test class [%s] with values %s", testClass.getName(), values));
		return (Class<?>) values.get(0);
	}

	/**
	 * @since 4.3
	 */
	private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
		ClassLoader classLoader = BootstrapUtils.class.getClassLoader();
		if (AnnotatedElementUtils.isAnnotated(testClass, WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME)) {
			return ClassUtils.forName(DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
		}
		return ClassUtils.forName(DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME, classLoader);
	}

}
