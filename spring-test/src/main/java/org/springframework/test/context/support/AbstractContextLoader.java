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

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * Abstract application context loader that provides a basis for all concrete
 * implementations of the {@link ContextLoader} SPI. Provides a
 * <em>Template Method</em> based approach for {@link #processLocations processing}
 * resource locations.
 *
 * <p>{@code AbstractContextLoader} also provides a basis for all concrete implementations
 * of the {@link SmartContextLoader} SPI. For backwards compatibility with the
 * {@code ContextLoader} SPI,
 * {@link #processContextConfiguration(ContextConfigurationAttributes)} delegates
 * to {@link #processLocations(Class, String...)}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.5
 * @see #generateDefaultLocations
 * @see #getResourceSuffixes
 * @see #modifyLocations
 * @see #prepareContext
 * @see #customizeContext
 */
public abstract class AbstractContextLoader implements SmartContextLoader {

	private static final String SLASH = "/";

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Log logger = LogFactory.getLog(AbstractContextLoader.class);


	// SmartContextLoader

	/**
	 * The default implementation processes locations analogous to
	 * {@link #processLocations(Class, String...)}, using the
	 * {@link ContextConfigurationAttributes#getDeclaringClass() declaring class}
	 * as the test class and the {@link ContextConfigurationAttributes#getLocations()
	 * resource locations} retrieved from the supplied
	 * {@link ContextConfigurationAttributes configuration attributes} as the
	 * locations to process. The processed locations are then
	 * {@link ContextConfigurationAttributes#setLocations(String[]) set} in
	 * the supplied configuration attributes.
	 * <p>Can be overridden in subclasses &mdash; for example, to process
	 * annotated classes instead of resource locations.
	 * @since 3.1
	 * @see #processLocations(Class, String...)
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		String[] processedLocations =
				processLocationsInternal(configAttributes.getDeclaringClass(), configAttributes.getLocations());
		configAttributes.setLocations(processedLocations);
	}

	/**
	 * Prepare the {@link ConfigurableApplicationContext} created by this
	 * {@code SmartContextLoader} <i>before</i> bean definitions are read.
	 * <p>The default implementation:
	 * <ul>
	 * <li>Sets the <em>active bean definition profiles</em> from the supplied
	 * {@code MergedContextConfiguration} in the
	 * {@link org.springframework.core.env.Environment Environment} of the
	 * context.</li>
	 * <li>Adds {@link PropertySource PropertySources} for all
	 * {@linkplain MergedContextConfiguration#getPropertySourceLocations()
	 * resource locations} and
	 * {@linkplain MergedContextConfiguration#getPropertySourceProperties()
	 * inlined properties} from the supplied {@code MergedContextConfiguration}
	 * to the {@code Environment} of the context.</li>
	 * <li>Determines what (if any) context initializer classes have been supplied
	 * via the {@code MergedContextConfiguration} and instantiates and
	 * {@linkplain ApplicationContextInitializer#initialize invokes} each with the
	 * given application context.
	 * <ul>
	 * <li>Any {@code ApplicationContextInitializers} implementing
	 * {@link org.springframework.core.Ordered Ordered} or annotated with {@link
	 * org.springframework.core.annotation.Order @Order} will be sorted appropriately.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * @param context the newly created application context
	 * @param mergedConfig the merged context configuration
	 * @since 3.2
	 * @see TestPropertySourceUtils#addPropertySourcesToEnvironment(ConfigurableApplicationContext, List)
	 * @see TestPropertySourceUtils#addInlinedPropertiesToEnvironment(ConfigurableApplicationContext, String...)
	 * @see ApplicationContextInitializer#initialize(ConfigurableApplicationContext)
	 * @see #loadContext(MergedContextConfiguration)
	 * @see ConfigurableApplicationContext#setId
	 */
	protected void prepareContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		context.getEnvironment().setActiveProfiles(mergedConfig.getActiveProfiles());
		TestPropertySourceUtils.addPropertySourcesToEnvironment(context, mergedConfig.getPropertySourceDescriptors());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, mergedConfig.getPropertySourceProperties());
		invokeApplicationContextInitializers(context, mergedConfig);
	}

	@SuppressWarnings("unchecked")
	private void invokeApplicationContextInitializers(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedConfig) {

		Set<Class<? extends ApplicationContextInitializer<?>>> initializerClasses =
				mergedConfig.getContextInitializerClasses();
		if (initializerClasses.isEmpty()) {
			// no ApplicationContextInitializers have been declared -> nothing to do
			return;
		}

		List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializerInstances = new ArrayList<>();
		Class<?> contextClass = context.getClass();

		for (Class<? extends ApplicationContextInitializer<?>> initializerClass : initializerClasses) {
			Class<?> initializerContextClass =
					GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
			if (initializerContextClass != null && !initializerContextClass.isInstance(context)) {
				throw new ApplicationContextException(String.format(
						"Could not apply context initializer [%s] since its generic parameter [%s] " +
						"is not assignable from the type of application context used by this " +
						"context loader: [%s]", initializerClass.getName(), initializerContextClass.getName(),
						contextClass.getName()));
			}
			initializerInstances.add((ApplicationContextInitializer<ConfigurableApplicationContext>) BeanUtils.instantiateClass(initializerClass));
		}

		AnnotationAwareOrderComparator.sort(initializerInstances);
		for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : initializerInstances) {
			initializer.initialize(context);
		}
	}

	/**
	 * Customize the {@link ConfigurableApplicationContext} created by this
	 * {@code ContextLoader} <em>after</em> bean definitions have been loaded
	 * into the context but <em>before</em> the context has been refreshed.
	 * <p>The default implementation delegates to all
	 * {@link MergedContextConfiguration#getContextCustomizers context customizers}
	 * that have been registered with the supplied {@code mergedConfig}.
	 * @param context the newly created application context
	 * @param mergedConfig the merged context configuration
	 * @since 4.3
	 */
	protected void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		for (ContextCustomizer contextCustomizer : mergedConfig.getContextCustomizers()) {
			contextCustomizer.customizeContext(context, mergedConfig);
		}
	}


	// ContextLoader

	/**
	 * If the supplied {@code locations} are {@code null} or <em>empty</em>
	 * and {@link #isGenerateDefaultLocations()} returns {@code true},
	 * default locations will be {@link #generateDefaultLocations(Class)
	 * generated} (i.e., detected) for the specified {@link Class class}
	 * and the configured {@linkplain #getResourceSuffixes() resource suffixes};
	 * otherwise, the supplied {@code locations} will be
	 * {@linkplain #modifyLocations modified} if necessary and returned.
	 * @param clazz the class with which the locations are associated: to be
	 * used when generating default locations
	 * @param locations the unmodified locations to use for loading the
	 * application context (can be {@code null} or empty)
	 * @return a processed array of application context resource locations
	 * @since 2.5
	 * @see #isGenerateDefaultLocations()
	 * @see #generateDefaultLocations(Class)
	 * @see #modifyLocations(Class, String...)
	 * @see org.springframework.test.context.ContextLoader#processLocations(Class, String...)
	 * @see #processContextConfiguration(ContextConfigurationAttributes)
	 */
	@Override
	@SuppressWarnings("deprecation")
	public final String[] processLocations(Class<?> clazz, String... locations) {
		return processLocationsInternal(clazz, locations);
	}

	private String[] processLocationsInternal(Class<?> clazz, String... locations) {
		return (ObjectUtils.isEmpty(locations) && isGenerateDefaultLocations()) ?
				generateDefaultLocations(clazz) : modifyLocations(clazz, locations);
	}

	/**
	 * Generate the default classpath resource locations array based on the
	 * supplied class.
	 * <p>For example, if the supplied class is {@code com.example.MyTest},
	 * the generated locations will contain a single string with a value of
	 * {@code "classpath:com/example/MyTest<suffix>"}, where {@code <suffix>}
	 * is the value of the first configured
	 * {@linkplain #getResourceSuffixes() resource suffix} for which the
	 * generated location actually exists in the classpath.
	 * <p>The implementation of this method adheres to the contract defined in the
	 * {@link SmartContextLoader} SPI. Specifically, this method will
	 * <em>preemptively</em> verify that the generated default location actually
	 * exists. If it does not exist, this method will log a warning and return an
	 * empty array.
	 * <p>Subclasses can override this method to implement a different
	 * <em>default location generation</em> strategy.
	 * @param clazz the class for which the default locations are to be generated
	 * @return an array of default application context resource locations
	 * @since 2.5
	 * @see #getResourceSuffixes()
	 */
	protected String[] generateDefaultLocations(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");

		String[] suffixes = getResourceSuffixes();
		for (String suffix : suffixes) {
			Assert.hasText(suffix, "Resource suffix must not be empty");
			String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName()) + suffix;
			ClassPathResource classPathResource = new ClassPathResource(resourcePath);
			if (classPathResource.exists()) {
				String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH + resourcePath;
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Detected default resource location \"%s\" for test class [%s]",
							prefixedResourcePath, clazz.getName()));
				}
				return new String[] {prefixedResourcePath};
			}
			else if (logger.isTraceEnabled()) {
				logger.trace(String.format("Did not detect default resource location for test class [%s]: " +
						"%s does not exist", clazz.getName(), classPathResource));
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Could not detect default resource locations for test class [%s]: " +
					"no resource found for suffixes %s.", clazz.getName(), ObjectUtils.nullSafeToString(suffixes)));
		}

		return EMPTY_STRING_ARRAY;
	}

	/**
	 * Generate a modified version of the supplied locations array and return it.
	 * <p>The default implementation simply delegates to
	 * {@link TestContextResourceUtils#convertToClasspathResourcePaths}.
	 * <p>Subclasses can override this method to implement a different
	 * <em>location modification</em> strategy.
	 * @param clazz the class with which the locations are associated
	 * @param locations the resource locations to be modified
	 * @return an array of modified application context resource locations
	 * @since 2.5
	 */
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		return TestContextResourceUtils.convertToClasspathResourcePaths(clazz, locations);
	}

	/**
	 * Determine whether <em>default</em> resource locations should be
	 * generated if the {@code locations} provided to
	 * {@link #processLocations(Class, String...)} are {@code null} or empty.
	 * <p>The semantics of this method have been overloaded to include detection
	 * of either default resource locations or default configuration classes.
	 * Consequently, this method can also be used to determine whether
	 * <em>default</em> configuration classes should be detected if the
	 * {@code classes} present in the {@linkplain ContextConfigurationAttributes
	 * configuration attributes} supplied to
	 * {@link #processContextConfiguration(ContextConfigurationAttributes)}
	 * are {@code null} or empty.
	 * <p>Can be overridden by subclasses to change the default behavior.
	 * @return always {@code true} by default
	 * @since 2.5
	 */
	protected boolean isGenerateDefaultLocations() {
		return true;
	}

	/**
	 * Get the suffixes to append to {@link ApplicationContext} resource locations
	 * when detecting default locations.
	 * <p>The default implementation simply wraps the value returned by
	 * {@link #getResourceSuffix()} in a single-element array, but this
	 * can be overridden by subclasses in order to support multiple suffixes.
	 * @return the resource suffixes; never {@code null} or empty
	 * @since 4.1
	 * @see #generateDefaultLocations(Class)
	 */
	protected String[] getResourceSuffixes() {
		return new String[] {getResourceSuffix()};
	}

	/**
	 * Get the suffix to append to {@link ApplicationContext} resource locations
	 * when detecting default locations.
	 * <p>Subclasses must provide an implementation of this method that returns
	 * a single suffix. Alternatively subclasses may provide a <em>no-op</em>
	 * implementation of this method and override {@link #getResourceSuffixes()}
	 * in order to provide multiple custom suffixes.
	 * @return the resource suffix; never {@code null} or empty
	 * @since 2.5
	 * @see #generateDefaultLocations(Class)
	 * @see #getResourceSuffixes()
	 */
	protected abstract String getResourceSuffix();

}
