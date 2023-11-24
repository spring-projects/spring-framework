/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.ClassUtils;

/**
 * {@code BootstrapUtils} is a collection of utility methods to assist with
 * bootstrapping the <em>Spring TestContext Framework</em>.
 *
 * <p>Only intended for internal use.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 * @see TestContextBootstrapper
 */
public abstract class BootstrapUtils {

	private static final String DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME =
			"org.springframework.test.context.support.DefaultBootstrapContext";

	private static final String DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME =
			"org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate";

	private static final String DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.support.DefaultTestContextBootstrapper";

	private static final String DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME =
			"org.springframework.test.context.web.WebTestContextBootstrapper";

	private static final String WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME =
			"org.springframework.test.context.web.WebAppConfiguration";

	private static final Class<? extends Annotation> webAppConfigurationClass = loadWebAppConfigurationClass();

	private static final Log logger = LogFactory.getLog(BootstrapUtils.class);


	/**
	 * Create the {@code BootstrapContext} for the specified {@linkplain Class test class}.
	 * <p>Uses reflection to create a {@link org.springframework.test.context.support.DefaultBootstrapContext}.
	 * that uses a {@link org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate}.
	 * @param testClass the test class for which the bootstrap context should be created
	 * @return a new {@code BootstrapContext}; never {@code null}
	 */
	@SuppressWarnings("unchecked")
	static BootstrapContext createBootstrapContext(Class<?> testClass) {
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = createCacheAwareContextLoaderDelegate();
		String className = DEFAULT_BOOTSTRAP_CONTEXT_CLASS_NAME;
		Class<? extends BootstrapContext> clazz;
		try {
			clazz = (Class<? extends BootstrapContext>)
					ClassUtils.forName(className, BootstrapUtils.class.getClassLoader());
			Constructor<? extends BootstrapContext> constructor =
					clazz.getConstructor(Class.class, CacheAwareContextLoaderDelegate.class);
			logger.trace(LogMessage.format("Instantiating BootstrapContext using constructor [%s]", constructor));
			return BeanUtils.instantiateClass(constructor, testClass, cacheAwareContextLoaderDelegate);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load BootstrapContext [%s]".formatted(className), ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static CacheAwareContextLoaderDelegate createCacheAwareContextLoaderDelegate() {
		String className = DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_CLASS_NAME;
		Class<? extends CacheAwareContextLoaderDelegate> clazz;
		try {
			clazz = (Class<? extends CacheAwareContextLoaderDelegate>)
					ClassUtils.forName(className, BootstrapUtils.class.getClassLoader());
			logger.trace(LogMessage.format("Instantiating CacheAwareContextLoaderDelegate from class [%s]", className));
			return BeanUtils.instantiateClass(clazz, CacheAwareContextLoaderDelegate.class);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not load CacheAwareContextLoaderDelegate [%s]".formatted(className), ex);
		}
	}

	/**
	 * Resolve the {@link TestContextBootstrapper} type for the supplied test class
	 * using the default {@link BootstrapContext}, instantiate the bootstrapper,
	 * and provide it a reference to the {@code BootstrapContext}.
	 * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
	 * the test class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type.
	 * Otherwise, either the
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} or the
	 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
	 * WebTestContextBootstrapper} will be used, depending on the presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}.
	 * @param testClass the test class for which the bootstrapper should be created
	 * @return a fully configured {@code TestContextBootstrapper}
	 * @since 6.0
	 */
	public static TestContextBootstrapper resolveTestContextBootstrapper(Class<?> testClass) {
		return resolveTestContextBootstrapper(createBootstrapContext(testClass));
	}

	/**
	 * Resolve the {@link TestContextBootstrapper} type for the test class in the
	 * supplied {@link BootstrapContext}, instantiate it, and provide it a reference
	 * to the {@link BootstrapContext}.
	 * <p>If the {@link BootstrapWith @BootstrapWith} annotation is present on
	 * the test class, either directly or as a meta-annotation, then its
	 * {@link BootstrapWith#value value} will be used as the bootstrapper type.
	 * Otherwise, either the
	 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper
	 * DefaultTestContextBootstrapper} or the
	 * {@link org.springframework.test.context.web.WebTestContextBootstrapper
	 * WebTestContextBootstrapper} will be used, depending on the presence of
	 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}.
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
			logger.trace(LogMessage.format("Instantiating TestContextBootstrapper for test class [%s] from class [%s]",
					testClass.getName(), clazz.getName()));
			TestContextBootstrapper testContextBootstrapper =
					BeanUtils.instantiateClass(clazz, TestContextBootstrapper.class);
			testContextBootstrapper.setBootstrapContext(bootstrapContext);
			return testContextBootstrapper;
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new IllegalStateException("""
					Could not load TestContextBootstrapper [%s]. Specify @BootstrapWith's 'value' \
					attribute or make the default bootstrapper class available.""".formatted(clazz), ex);
		}
	}

	@Nullable
	private static Class<?> resolveExplicitTestContextBootstrapper(Class<?> testClass) {
		Set<BootstrapWith> annotations = new LinkedHashSet<>();
		AnnotationDescriptor<BootstrapWith> descriptor =
				TestContextAnnotationUtils.findAnnotationDescriptor(testClass, BootstrapWith.class);
		while (descriptor != null) {
			annotations.addAll(descriptor.findAllLocalMergedAnnotations());
			descriptor = descriptor.next();
		}

		if (annotations.isEmpty()) {
			return null;
		}
		if (annotations.size() == 1) {
			return annotations.iterator().next().value();
		}

		// Allow directly-present annotation to override annotations that are meta-present.
		BootstrapWith bootstrapWith = testClass.getDeclaredAnnotation(BootstrapWith.class);
		if (bootstrapWith != null) {
			return bootstrapWith.value();
		}

		throw new IllegalStateException(String.format(
				"Configuration error: found multiple declarations of @BootstrapWith for test class [%s]: %s",
				testClass.getName(), annotations));
	}

	private static Class<?> resolveDefaultTestContextBootstrapper(Class<?> testClass) throws Exception {
		boolean webApp = TestContextAnnotationUtils.hasAnnotation(testClass, webAppConfigurationClass);
		String bootstrapperClassName = (webApp ? DEFAULT_WEB_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME :
				DEFAULT_TEST_CONTEXT_BOOTSTRAPPER_CLASS_NAME);
		return ClassUtils.forName(bootstrapperClassName, BootstrapUtils.class.getClassLoader());
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends Annotation> loadWebAppConfigurationClass() {
		try {
			return (Class<? extends Annotation>) ClassUtils.forName(WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME,
				BootstrapUtils.class.getClassLoader());
		}
		catch (ClassNotFoundException | LinkageError ex) {
			throw new IllegalStateException(
				"Failed to load class for @" + WEB_APP_CONFIGURATION_ANNOTATION_CLASS_NAME, ex);
		}
	}

}
