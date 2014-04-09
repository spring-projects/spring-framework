/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract implementation of the {@link TestContextBootstrapper} interface which
 * provides most of the behavior required by a bootstrapper.
 *
 * <p>Concrete subclasses typically will only need to provide implementations for
 * the following {@code abstract} methods:
 * <ul>
 * <li>{@link #getDefaultTestExecutionListenerClassNames()}
 * <li>{@link #getDefaultContextLoaderClass(Class)}
 * <li>{@link #buildMergedContextConfiguration(Class, String[], Class[], Set, String[], ContextLoader, CacheAwareContextLoaderDelegate, MergedContextConfiguration)}
 *
 * @author Sam Brannen
 * @since 4.1
 */
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {

	private static final Log logger = LogFactory.getLog(AbstractTestContextBootstrapper.class);

	private BootstrapContext bootstrapContext;


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBootstrapContext(BootstrapContext bootstrapContext) {
		this.bootstrapContext = bootstrapContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BootstrapContext getBootstrapContext() {
		return this.bootstrapContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final List<TestExecutionListener> getTestExecutionListeners() {
		Class<?> clazz = getBootstrapContext().getTestClass();
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<Class<? extends TestExecutionListener>>();

		AnnotationDescriptor<TestExecutionListeners> descriptor = MetaAnnotationUtils.findAnnotationDescriptor(clazz,
			annotationType);

		// Use defaults?
		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("@TestExecutionListeners is not present for class [" + clazz + "]: using defaults.");
			}
			classesList.addAll(getDefaultTestExecutionListenerClasses());
		}
		else {
			// Traverse the class hierarchy...
			while (descriptor != null) {
				Class<?> declaringClass = descriptor.getDeclaringClass();

				AnnotationAttributes annAttrs = descriptor.getAnnotationAttributes();
				if (logger.isTraceEnabled()) {
					logger.trace(String.format(
						"Retrieved @TestExecutionListeners attributes [%s] for declaring class [%s].", annAttrs,
						declaringClass));
				}

				Class<? extends TestExecutionListener>[] valueListenerClasses = (Class<? extends TestExecutionListener>[]) annAttrs.getClassArray("value");
				Class<? extends TestExecutionListener>[] listenerClasses = (Class<? extends TestExecutionListener>[]) annAttrs.getClassArray("listeners");
				if (!ObjectUtils.isEmpty(valueListenerClasses) && !ObjectUtils.isEmpty(listenerClasses)) {
					String msg = String.format(
						"Class [%s] has been configured with @TestExecutionListeners' 'value' [%s] "
								+ "and 'listeners' [%s] attributes. Use one or the other, but not both.",
						declaringClass, ObjectUtils.nullSafeToString(valueListenerClasses),
						ObjectUtils.nullSafeToString(listenerClasses));
					logger.error(msg);
					throw new IllegalStateException(msg);
				}
				else if (!ObjectUtils.isEmpty(valueListenerClasses)) {
					listenerClasses = valueListenerClasses;
				}

				if (listenerClasses != null) {
					classesList.addAll(0, Arrays.<Class<? extends TestExecutionListener>> asList(listenerClasses));
				}

				descriptor = (annAttrs.getBoolean("inheritListeners") ? MetaAnnotationUtils.findAnnotationDescriptor(
					descriptor.getRootDeclaringClass().getSuperclass(), annotationType) : null);
			}
		}

		List<TestExecutionListener> listeners = new ArrayList<TestExecutionListener>(classesList.size());
		for (Class<? extends TestExecutionListener> listenerClass : classesList) {
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			}
			catch (NoClassDefFoundError err) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Could not instantiate TestExecutionListener [%s]. "
							+ "Specify custom listener classes or make the default listener classes "
							+ "(and their dependencies) available.", listenerClass.getName()));
				}
			}
		}
		return listeners;
	}

	/**
	 * Get the default {@link TestExecutionListener} classes for this bootstrapper.
	 * <p>This method is invoked by {@link #getTestExecutionListeners()} and
	 * delegates to {@link #getDefaultTestExecutionListenerClassNames()} to
	 * retrieve the class names.
	 * <p>If a particular class cannot be loaded, a {@code DEBUG} message will
	 * be logged, but the associated exception will not be rethrown.
	 */
	@SuppressWarnings("unchecked")
	protected Set<Class<? extends TestExecutionListener>> getDefaultTestExecutionListenerClasses() {
		Set<Class<? extends TestExecutionListener>> defaultListenerClasses = new LinkedHashSet<Class<? extends TestExecutionListener>>();
		for (String className : getDefaultTestExecutionListenerClassNames()) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) getClass().getClassLoader().loadClass(
					className));
			}
			catch (Throwable t) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className
							+ "]. Specify custom listener classes or make the default listener classes available.", t);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({ "unchecked" })
	@Override
	public final MergedContextConfiguration buildMergedContextConfiguration() {
		Class<?> testClass = getBootstrapContext().getTestClass();
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getBootstrapContext().getCacheAwareContextLoaderDelegate();

		if (MetaAnnotationUtils.findAnnotationDescriptorForTypes(testClass, ContextConfiguration.class,
			ContextHierarchy.class) == null) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format(
					"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s]",
					testClass.getName()));
			}
			return new MergedContextConfiguration(testClass, null, null, null, null);
		}

		if (AnnotationUtils.findAnnotation(testClass, ContextHierarchy.class) != null) {
			Map<String, List<ContextConfigurationAttributes>> hierarchyMap = ContextLoaderUtils.buildContextHierarchyMap(testClass);

			MergedContextConfiguration parentConfig = null;
			MergedContextConfiguration mergedConfig = null;

			for (List<ContextConfigurationAttributes> list : hierarchyMap.values()) {
				List<ContextConfigurationAttributes> reversedList = new ArrayList<ContextConfigurationAttributes>(list);
				Collections.reverse(reversedList);

				// Don't use the supplied testClass; instead ensure that we are
				// building the MCC for the actual test class that declared the
				// configuration for the current level in the context hierarchy.
				Assert.notEmpty(reversedList, "ContextConfigurationAttributes list must not be empty");
				Class<?> declaringClass = reversedList.get(0).getDeclaringClass();

				mergedConfig = buildMergedContextConfiguration(declaringClass, reversedList, parentConfig,
					cacheAwareContextLoaderDelegate);
				parentConfig = mergedConfig;
			}

			// Return the last level in the context hierarchy
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass,
				ContextLoaderUtils.resolveContextConfigurationAttributes(testClass), null,
				cacheAwareContextLoaderDelegate);
		}
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration}
	 * for the supplied {@link Class testClass}, context configuration attributes,
	 * and parent context configuration.
	 *
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param configAttributesList the list of context configuration attributes for the
	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
	 * traversing up the class hierarchy); never {@code null} or empty
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @return the merged context configuration
	 * @see #resolveContextLoader
	 * @see ContextLoaderUtils#resolveContextConfigurationAttributes
	 * @see SmartContextLoader#processContextConfiguration
	 * @see ContextLoader#processLocations
	 * @see ActiveProfilesUtils#resolveActiveProfiles
	 * @see ApplicationContextInitializerUtils#resolveInitializerClasses
	 * @see MergedContextConfiguration
	 */
	private MergedContextConfiguration buildMergedContextConfiguration(final Class<?> testClass,
			final List<ContextConfigurationAttributes> configAttributesList, MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		final ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
		final List<String> locationsList = new ArrayList<String>();
		final List<Class<?>> classesList = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
					configAttributes));
			}

			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locationsList.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classesList.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(configAttributes.getDeclaringClass(),
					configAttributes.getLocations());
				locationsList.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}

			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		String[] locations = StringUtils.toStringArray(locationsList);
		Class<?>[] classes = ClassUtils.toClassArray(classesList);
		Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses = //
		ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList);
		String[] activeProfiles = ActiveProfilesUtils.resolveActiveProfiles(testClass);

		return buildMergedContextConfiguration(testClass, locations, classes, initializerClasses, activeProfiles,
			contextLoader, cacheAwareContextLoaderDelegate, parentConfig);
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the
	 * supplied list of {@link ContextConfigurationAttributes} and then instantiate
	 * and return that {@code ContextLoader}.
	 *
	 * <p>If the user has not explicitly declared which loader to use, the value
	 * returned from {@link #getDefaultContextLoaderClass} will be used as the
	 * default context loader class. For details on the class resolution process,
	 * see {@link #resolveExplicitContextLoaderClass} and
	 * {@link #getDefaultContextLoaderClass}.
	 *
	 * @param testClass the test class for which the {@code ContextLoader} should be
	 * resolved; must not be {@code null}
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
	 * (never {@code null})
	 */
	private ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {
		Assert.notNull(testClass, "Class must not be null");
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");

		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
		if (contextLoaderClass == null) {
			contextLoaderClass = getDefaultContextLoaderClass(testClass);
		}

		if (logger.isTraceEnabled()) {
			logger.trace(String.format("Using ContextLoader class [%s] for test class [%s]",
				contextLoaderClass.getName(), testClass.getName()));
		}

		return BeanUtils.instantiateClass(contextLoaderClass, ContextLoader.class);
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the supplied
	 * list of {@link ContextConfigurationAttributes}.
	 *
	 * <p>Beginning with the first level in the context configuration attributes hierarchy:
	 *
	 * <ol>
	 * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
	 * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
	 * configured with an explicit class, that class will be returned.</li>
	 * <li>If an explicit {@code ContextLoader} class is not specified at the current
	 * level in the hierarchy, traverse to the next level in the hierarchy and return to
	 * step #1.</li>
	 * </ol>
	 *
	 * @param configAttributesList the list of configuration attributes to process;
	 * must not be {@code null} or <em>empty</em>; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the {@code ContextLoader} class to use for the supplied configuration
	 * attributes, or {@code null} if no explicit loader is found
	 * @throws IllegalArgumentException if supplied configuration attributes are
	 * {@code null} or <em>empty</em>
	 */
	private Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
			List<ContextConfigurationAttributes> configAttributesList) {
		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be empty");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Resolving ContextLoader for context configuration attributes %s",
					configAttributes));
			}

			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (!ContextLoader.class.equals(contextLoaderClass)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Found explicit ContextLoader class [%s] for context configuration attributes %s",
						contextLoaderClass.getName(), configAttributes));
				}
				return contextLoaderClass;
			}
		}

		return null;
	}

	/**
	 * Get the names of the default {@link TestExecutionListener} classes for
	 * this bootstrapper.
	 * <p>This method is invoked by {@link #getDefaultTestExecutionListenerClasses()}.
	 * @return an <em>unmodifiable</em> list of names of default {@code
	 * TestExecutionListener} classes
	 */
	protected abstract List<String> getDefaultTestExecutionListenerClassNames();

	/**
	 * Determine the default {@link ContextLoader} class to use for the supplied
	 * test class.
	 * <p>The class returned by this method will only be used if a {@code ContextLoader}
	 * class has not been explicitly declared via {@link ContextConfiguration#loader}.
	 * @param testClass the test class for which to retrieve the default
	 * {@code ContextLoader} class
	 */
	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

	/**
	 * Build a {@link MergedContextConfiguration} instance from the supplied,
	 * merged values.
	 *
	 * <p>Concrete subclasses typically will only need to instantiate
	 * {@link MergedContextConfiguration} (or a specialized subclass thereof)
	 * from the provided values; further processing and merging of values is likely
	 * unnecessary.
	 *
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param locations the merged resource locations
	 * @param classes the merged annotated classes
	 * @param initializerClasses the merged context initializer classes
	 * @param activeProfiles the merged active bean definition profiles
	 * @param contextLoader the resolved {@code ContextLoader}
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be provided to the instantiated {@code MergedContextConfiguration}
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @return the fully initialized {@code MergedContextConfiguration}
	 */
	protected abstract MergedContextConfiguration buildMergedContextConfiguration(
			Class<?> testClass,
			String[] locations,
			Class<?>[] classes,
			Set<Class<? extends ApplicationContextInitializer<? extends ConfigurableApplicationContext>>> initializerClasses,
			String[] activeProfiles, ContextLoader contextLoader,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate, MergedContextConfiguration parentConfig);

}
