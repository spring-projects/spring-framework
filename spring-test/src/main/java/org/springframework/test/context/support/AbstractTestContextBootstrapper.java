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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.util.MetaAnnotationUtils;
import org.springframework.test.util.MetaAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract implementation of the {@link TestContextBootstrapper} interface which
 * provides most of the behavior required by a bootstrapper.
 *
 * <p>Concrete subclasses typically will only need to provide implementations for
 * the following methods:
 * <ul>
 * <li>{@link #getDefaultContextLoaderClass}
 * <li>{@link #processMergedContextConfiguration}
 * </ul>
 *
 * <p>To plug in custom
 * {@link org.springframework.test.context.cache.ContextCache ContextCache}
 * support, override {@link #getCacheAwareContextLoaderDelegate()}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.1
 */
public abstract class AbstractTestContextBootstrapper implements TestContextBootstrapper {

	private final Log logger = LogFactory.getLog(getClass());

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
	 * Build a new {@link DefaultTestContext} using the {@linkplain Class test class}
	 * in the {@link BootstrapContext} associated with this bootstrapper and
	 * by delegating to {@link #buildMergedContextConfiguration()} and
	 * {@link #getCacheAwareContextLoaderDelegate()}.
	 * <p>Concrete subclasses may choose to override this method to return a
	 * custom {@link TestContext} implementation.
	 * @since 4.2
	 */
	@Override
	public TestContext buildTestContext() {
		return new DefaultTestContext(getBootstrapContext().getTestClass(), buildMergedContextConfiguration(),
			getCacheAwareContextLoaderDelegate());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final List<TestExecutionListener> getTestExecutionListeners() {
		Class<?> clazz = getBootstrapContext().getTestClass();
		Class<TestExecutionListeners> annotationType = TestExecutionListeners.class;
		List<Class<? extends TestExecutionListener>> classesList = new ArrayList<Class<? extends TestExecutionListener>>();
		boolean usingDefaults = false;

		AnnotationDescriptor<TestExecutionListeners> descriptor = MetaAnnotationUtils.findAnnotationDescriptor(clazz,
			annotationType);

		// Use defaults?
		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("@TestExecutionListeners is not present for class [%s]: using defaults.",
					clazz.getName()));
			}
			usingDefaults = true;
			classesList.addAll(getDefaultTestExecutionListenerClasses());
		}
		else {
			// Traverse the class hierarchy...
			while (descriptor != null) {
				Class<?> declaringClass = descriptor.getDeclaringClass();
				TestExecutionListeners testExecutionListeners = descriptor.synthesizeAnnotation();
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("Retrieved @TestExecutionListeners [%s] for declaring class [%s].",
						testExecutionListeners, declaringClass.getName()));
				}

				boolean inheritListeners = testExecutionListeners.inheritListeners();
				AnnotationDescriptor<TestExecutionListeners> superDescriptor = MetaAnnotationUtils.findAnnotationDescriptor(
					descriptor.getRootDeclaringClass().getSuperclass(), annotationType);

				// If there are no listeners to inherit, we might need to merge the
				// locally declared listeners with the defaults.
				if ((!inheritListeners || superDescriptor == null)
						&& (testExecutionListeners.mergeMode() == MergeMode.MERGE_WITH_DEFAULTS)) {
					if (logger.isDebugEnabled()) {
						logger.debug(String.format(
							"Merging default listeners with listeners configured via @TestExecutionListeners for class [%s].",
							descriptor.getRootDeclaringClass().getName()));
					}
					usingDefaults = true;
					classesList.addAll(getDefaultTestExecutionListenerClasses());
				}

				classesList.addAll(0, Arrays.asList(testExecutionListeners.listeners()));

				descriptor = (inheritListeners ? superDescriptor : null);
			}
		}

		// Remove possible duplicates if we loaded default listeners.
		if (usingDefaults) {
			Set<Class<? extends TestExecutionListener>> classesSet = new HashSet<Class<? extends TestExecutionListener>>();
			classesSet.addAll(classesList);
			classesList.clear();
			classesList.addAll(classesSet);
		}

		List<TestExecutionListener> listeners = instantiateListeners(classesList);

		// Sort by Ordered/@Order if we loaded default listeners.
		if (usingDefaults) {
			AnnotationAwareOrderComparator.sort(listeners);
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Using TestExecutionListeners: %s", listeners));
		}
		return listeners;
	}

	private List<TestExecutionListener> instantiateListeners(List<Class<? extends TestExecutionListener>> classesList) {
		List<TestExecutionListener> listeners = new ArrayList<TestExecutionListener>(classesList.size());
		for (Class<? extends TestExecutionListener> listenerClass : classesList) {
			NoClassDefFoundError ncdfe = null;
			try {
				listeners.add(BeanUtils.instantiateClass(listenerClass));
			}
			catch (NoClassDefFoundError err) {
				ncdfe = err;
			}
			catch (BeanInstantiationException ex) {
				if (ex.getCause() instanceof NoClassDefFoundError) {
					ncdfe = (NoClassDefFoundError) ex.getCause();
				}
			}
			if (ncdfe != null) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Could not instantiate TestExecutionListener [%s]. "
							+ "Specify custom listener classes or make the default listener classes "
							+ "(and their required dependencies) available. Offending class: [%s]",
						listenerClass.getName(), ncdfe.getMessage()));
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
		ClassLoader cl = getClass().getClassLoader();
		for (String className : getDefaultTestExecutionListenerClassNames()) {
			try {
				defaultListenerClasses.add((Class<? extends TestExecutionListener>) ClassUtils.forName(className, cl));
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not load default TestExecutionListener class [" + className
							+ "]. Specify custom listener classes or make the default listener classes available.", ex);
				}
			}
		}
		return defaultListenerClasses;
	}

	/**
	 * Get the names of the default {@link TestExecutionListener} classes for
	 * this bootstrapper.
	 * <p>The default implementation looks up all
	 * {@code org.springframework.test.context.TestExecutionListener} entries
	 * configured in all {@code META-INF/spring.factories} files on the classpath.
	 * <p>This method is invoked by {@link #getDefaultTestExecutionListenerClasses()}.
	 * @return an <em>unmodifiable</em> list of names of default {@code TestExecutionListener}
	 * classes
	 * @see SpringFactoriesLoader#loadFactoryNames
	 */
	protected List<String> getDefaultTestExecutionListenerClassNames() {
		final List<String> classNames = SpringFactoriesLoader.loadFactoryNames(TestExecutionListener.class,
			getClass().getClassLoader());

		if (logger.isInfoEnabled()) {
			logger.info(String.format("Loaded default TestExecutionListener class names from location [%s]: %s",
				SpringFactoriesLoader.FACTORIES_RESOURCE_LOCATION, classNames));
		}
		return Collections.unmodifiableList(classNames);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final MergedContextConfiguration buildMergedContextConfiguration() {
		Class<?> testClass = getBootstrapContext().getTestClass();
		CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate = getCacheAwareContextLoaderDelegate();

		if (MetaAnnotationUtils.findAnnotationDescriptorForTypes(testClass, ContextConfiguration.class,
				ContextHierarchy.class) == null) {
			return buildDefaultMergedContextConfiguration(testClass, cacheAwareContextLoaderDelegate);
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
					cacheAwareContextLoaderDelegate, true);
				parentConfig = mergedConfig;
			}

			// Return the last level in the context hierarchy
			return mergedConfig;
		}
		else {
			return buildMergedContextConfiguration(testClass,
				ContextLoaderUtils.resolveContextConfigurationAttributes(testClass), null,
				cacheAwareContextLoaderDelegate, true);
		}
	}

	/**
	 * @since 4.3
	 */
	private MergedContextConfiguration buildDefaultMergedContextConfiguration(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		List<ContextConfigurationAttributes> defaultConfigAttributesList
			= Collections.singletonList(new ContextConfigurationAttributes(testClass));

		ContextLoader contextLoader = resolveContextLoader(testClass, defaultConfigAttributesList);
		if (logger.isInfoEnabled()) {
			logger.info(String.format(
				"Neither @ContextConfiguration nor @ContextHierarchy found for test class [%s], using %s",
				testClass.getName(), contextLoader.getClass().getSimpleName()));
		}
		return buildMergedContextConfiguration(testClass, defaultConfigAttributesList, null,
			cacheAwareContextLoaderDelegate, false);
	}

	/**
	 * Build the {@link MergedContextConfiguration merged context configuration}
	 * for the supplied {@link Class testClass}, context configuration attributes,
	 * and parent context configuration.
	 * @param testClass the test class for which the {@code MergedContextConfiguration}
	 * should be built (must not be {@code null})
	 * @param configAttributesList the list of context configuration attributes for the
	 * specified test class, ordered <em>bottom-up</em> (i.e., as if we were
	 * traversing up the class hierarchy); never {@code null} or empty
	 * @param parentConfig the merged context configuration for the parent application
	 * context in a context hierarchy, or {@code null} if there is no parent
	 * @param cacheAwareContextLoaderDelegate the cache-aware context loader delegate to
	 * be passed to the {@code MergedContextConfiguration} constructor
	 * @param requireLocationsClassesOrInitializers whether locations, classes, or
	 * initializers are required; typically {@code true} but may be set to {@code false}
	 * if the configured loader supports empty configuration
	 * @return the merged context configuration
	 * @see #resolveContextLoader
	 * @see ContextLoaderUtils#resolveContextConfigurationAttributes
	 * @see SmartContextLoader#processContextConfiguration
	 * @see ContextLoader#processLocations
	 * @see ActiveProfilesUtils#resolveActiveProfiles
	 * @see ApplicationContextInitializerUtils#resolveInitializerClasses
	 * @see MergedContextConfiguration
	 */
	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList, MergedContextConfiguration parentConfig,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate,
			boolean requireLocationsClassesOrInitializers) {

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes list must not be null or empty");

		ContextLoader contextLoader = resolveContextLoader(testClass, configAttributesList);
		List<String> locations = new ArrayList<String>();
		List<Class<?>> classes = new ArrayList<Class<?>>();
		List<Class<?>> initializers = new ArrayList<Class<?>>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Processing locations and classes for context configuration attributes %s",
					configAttributes));
			}
			if (contextLoader instanceof SmartContextLoader) {
				SmartContextLoader smartContextLoader = (SmartContextLoader) contextLoader;
				smartContextLoader.processContextConfiguration(configAttributes);
				locations.addAll(0, Arrays.asList(configAttributes.getLocations()));
				classes.addAll(0, Arrays.asList(configAttributes.getClasses()));
			}
			else {
				String[] processedLocations = contextLoader.processLocations(
						configAttributes.getDeclaringClass(), configAttributes.getLocations());
				locations.addAll(0, Arrays.asList(processedLocations));
				// Legacy ContextLoaders don't know how to process classes
			}
			initializers.addAll(0, Arrays.asList(configAttributes.getInitializers()));
			if (!configAttributes.isInheritLocations()) {
				break;
			}
		}

		Set<ContextCustomizer> contextCustomizers = getContextCustomizers(testClass,
				Collections.unmodifiableList(configAttributesList));

		if (requireLocationsClassesOrInitializers && areAllEmpty(locations, classes, initializers, contextCustomizers)) {
			throw new IllegalStateException(String.format(
					"%s was unable to detect defaults, and no ApplicationContextInitializers "
							+ "or ContextCustomizers were declared for context configuration attributes %s",
					contextLoader.getClass().getSimpleName(), configAttributesList));
		}

		MergedTestPropertySources mergedTestPropertySources = TestPropertySourceUtils.buildMergedTestPropertySources(testClass);
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(testClass,
				StringUtils.toStringArray(locations),
				ClassUtils.toClassArray(classes),
				ApplicationContextInitializerUtils.resolveInitializerClasses(configAttributesList),
				ActiveProfilesUtils.resolveActiveProfiles(testClass),
				mergedTestPropertySources.getLocations(),
				mergedTestPropertySources.getProperties(),
				contextCustomizers, contextLoader, cacheAwareContextLoaderDelegate, parentConfig);

		return processMergedContextConfiguration(mergedConfig);
	}

	/**
	 * @since 4.3
	 */
	private Set<ContextCustomizer> getContextCustomizers(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		List<ContextCustomizerFactory> factories = getContextCustomizerFactories();
		Set<ContextCustomizer> customizers = new LinkedHashSet<ContextCustomizer>(factories.size());
		for (ContextCustomizerFactory factory : factories) {
			ContextCustomizer customizer = factory.createContextCustomizer(testClass, configAttributes);
			if (customizer != null) {
				customizers.add(customizer);
			}
		}
		return customizers;
	}

	/**
	 * Get the {@link ContextCustomizerFactory} instances for this bootstrapper.
	 * <p>The default implementation uses the {@link SpringFactoriesLoader} mechanism
	 * for loading factories configured in all {@code META-INF/spring.factories}
	 * files on the classpath.
	 * @since 4.3
	 * @see SpringFactoriesLoader#loadFactories
	 */
	protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
		return SpringFactoriesLoader.loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader());
	}

	/**
	 * @since 4.3
	 */
	private boolean areAllEmpty(Collection<?>... collections) {
		for (Collection<?> collection : collections) {
			if (!collection.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Resolve the {@link ContextLoader} {@linkplain Class class} to use for the
	 * supplied list of {@link ContextConfigurationAttributes} and then instantiate
	 * and return that {@code ContextLoader}.
	 * <p>If the user has not explicitly declared which loader to use, the value
	 * returned from {@link #getDefaultContextLoaderClass} will be used as the
	 * default context loader class. For details on the class resolution process,
	 * see {@link #resolveExplicitContextLoaderClass} and
	 * {@link #getDefaultContextLoaderClass}.
	 * @param testClass the test class for which the {@code ContextLoader} should be
	 * resolved; must not be {@code null}
	 * @param configAttributesList the list of configuration attributes to process; must
	 * not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the resolved {@code ContextLoader} for the supplied {@code testClass}
	 * (never {@code null})
	 * @throws IllegalStateException if {@link #getDefaultContextLoaderClass(Class)}
	 * returns {@code null}
	 */
	protected ContextLoader resolveContextLoader(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(testClass, "Class must not be null");
		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		Class<? extends ContextLoader> contextLoaderClass = resolveExplicitContextLoaderClass(configAttributesList);
		if (contextLoaderClass == null) {
			contextLoaderClass = getDefaultContextLoaderClass(testClass);
			if (contextLoaderClass == null) {
				throw new IllegalStateException("getDefaultContextLoaderClass() must not return null");
			}
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
	 * <p>Beginning with the first level in the context configuration attributes hierarchy:
	 * <ol>
	 * <li>If the {@link ContextConfigurationAttributes#getContextLoaderClass()
	 * contextLoaderClass} property of {@link ContextConfigurationAttributes} is
	 * configured with an explicit class, that class will be returned.</li>
	 * <li>If an explicit {@code ContextLoader} class is not specified at the current
	 * level in the hierarchy, traverse to the next level in the hierarchy and return to
	 * step #1.</li>
	 * </ol>
	 * @param configAttributesList the list of configuration attributes to process;
	 * must not be {@code null}; must be ordered <em>bottom-up</em>
	 * (i.e., as if we were traversing up the class hierarchy)
	 * @return the {@code ContextLoader} class to use for the supplied configuration
	 * attributes, or {@code null} if no explicit loader is found
	 * @throws IllegalArgumentException if supplied configuration attributes are
	 * {@code null} or <em>empty</em>
	 */
	protected Class<? extends ContextLoader> resolveExplicitContextLoaderClass(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notNull(configAttributesList, "ContextConfigurationAttributes list must not be null");

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Resolving ContextLoader for context configuration attributes %s",
					configAttributes));
			}
			Class<? extends ContextLoader> contextLoaderClass = configAttributes.getContextLoaderClass();
			if (ContextLoader.class != contextLoaderClass) {
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
	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
	 * interaction with the {@code ContextCache}.
	 * <p>The default implementation simply delegates to
	 * {@code getBootstrapContext().getCacheAwareContextLoaderDelegate()}.
	 * <p>Concrete subclasses may choose to override this method to return a custom
	 * {@code CacheAwareContextLoaderDelegate} implementation with custom
	 * {@link org.springframework.test.context.cache.ContextCache ContextCache} support.
	 * @return the context loader delegate (never {@code null})
	 */
	protected CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return getBootstrapContext().getCacheAwareContextLoaderDelegate();
	}

	/**
	 * Determine the default {@link ContextLoader} {@linkplain Class class}
	 * to use for the supplied test class.
	 * <p>The class returned by this method will only be used if a {@code ContextLoader}
	 * class has not been explicitly declared via {@link ContextConfiguration#loader}.
	 * @param testClass the test class for which to retrieve the default
	 * {@code ContextLoader} class
	 * @return the default {@code ContextLoader} class for the supplied test class
	 * (never {@code null})
	 */
	protected abstract Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass);

	/**
	 * Process the supplied, newly instantiated {@link MergedContextConfiguration} instance.
	 * <p>The returned {@link MergedContextConfiguration} instance may be a wrapper
	 * around or a replacement for the original.
	 * <p>The default implementation simply returns the supplied instance unmodified.
	 * <p>Concrete subclasses may choose to return a specialized subclass of
	 * {@link MergedContextConfiguration} based on properties in the supplied instance.
	 * @param mergedConfig the {@code MergedContextConfiguration} to process; never {@code null}
	 * @return a fully initialized {@code MergedContextConfiguration}; never {@code null}
	 */
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return mergedConfig;
	}

}
