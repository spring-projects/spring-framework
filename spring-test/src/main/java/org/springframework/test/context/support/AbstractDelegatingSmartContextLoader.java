/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * {@code AbstractDelegatingSmartContextLoader} serves as an abstract base class
 * for implementations of the {@link SmartContextLoader} SPI that delegate to a
 * set of <em>candidate</em> SmartContextLoaders (i.e., one that supports XML
 * configuration files and one that supports annotated classes) to determine which
 * context loader is appropriate for a given test class's configuration. Each
 * candidate is given a chance to {@link #processContextConfiguration process} the
 * {@link ContextConfigurationAttributes} for each class in the test class hierarchy
 * that is annotated with {@link ContextConfiguration @ContextConfiguration}, and
 * the candidate that supports the merged, processed configuration will be used to
 * actually {@link #loadContext load} the context.
 * 
 * <p>Placing an empty {@code @ContextConfiguration} annotation on a test class signals
 * that default resource locations (i.e., XML configuration files) or default
 * {@link org.springframework.context.annotation.Configuration configuration classes}
 * should be detected. Furthermore, if a specific {@link ContextLoader} or
 * {@link SmartContextLoader} is not explicitly declared via
 * {@code @ContextConfiguration}, a concrete subclass of
 * {@code AbstractDelegatingSmartContextLoader} will be used as the default loader,
 * thus providing automatic support for either XML configuration files or annotated
 * classes, but not both simultaneously.
 * 
 * <p>As of Spring 3.2, a test class may optionally declare neither XML configuration
 * files nor annotated classes and instead declare only {@linkplain
 * ContextConfiguration#initializers application context initializers}. In such
 * cases, an attempt will still be made to detect defaults, but their absence will
 * not result an an exception.
 * 
 * @author Sam Brannen
 * @since 3.2
 * @see SmartContextLoader
 */
public abstract class AbstractDelegatingSmartContextLoader implements SmartContextLoader {

	private static final Log logger = LogFactory.getLog(AbstractDelegatingSmartContextLoader.class);


	/**
	 * Get the delegate {@code SmartContextLoader} that supports XML configuration files.
	 */
	protected abstract SmartContextLoader getXmlLoader();

	/**
	 * Get the delegate {@code SmartContextLoader} that supports annotated classes.
	 */
	protected abstract SmartContextLoader getAnnotationConfigLoader();

	// --- SmartContextLoader --------------------------------------------------

	private static String name(SmartContextLoader loader) {
		return loader.getClass().getSimpleName();
	}

	private static void delegateProcessing(SmartContextLoader loader, ContextConfigurationAttributes configAttributes) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to process context configuration %s.", name(loader),
				configAttributes));
		}
		loader.processContextConfiguration(configAttributes);
	}

	private static ApplicationContext delegateLoading(SmartContextLoader loader, MergedContextConfiguration mergedConfig)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Delegating to %s to load context from %s.", name(loader), mergedConfig));
		}
		return loader.loadContext(mergedConfig);
	}

	private boolean supports(SmartContextLoader loader, MergedContextConfiguration mergedConfig) {
		if (loader == getAnnotationConfigLoader()) {
			return ObjectUtils.isEmpty(mergedConfig.getLocations()) && !ObjectUtils.isEmpty(mergedConfig.getClasses());
		} else {
			return !ObjectUtils.isEmpty(mergedConfig.getLocations()) && ObjectUtils.isEmpty(mergedConfig.getClasses());
		}
	}

	/**
	 * Delegates to candidate {@code SmartContextLoaders} to process the supplied
	 * {@link ContextConfigurationAttributes}.
	 * 
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders for {@link #getXmlLoader() XML configuration files} and
	 * {@link #getAnnotationConfigLoader() annotated classes}. Specifically, the
	 * delegation algorithm is as follows:
	 * 
	 * <ul>
	 * <li>If the resource locations or annotated classes in the supplied
	 * {@code ContextConfigurationAttributes} are not empty, the appropriate 
	 * candidate loader will be allowed to process the configuration <em>as is</em>,
	 * without any checks for detection of defaults.</li>
	 * <li>Otherwise, the XML-based loader will be allowed to process
	 * the configuration in order to detect default resource locations. If
	 * the XML-based loader detects default resource locations,
	 * an {@code info} message will be logged.</li>
	 * <li>Subsequently, the annotation-based loader will be allowed to
	 * process the configuration in order to detect default configuration classes.
	 * If the annotation-based loader detects default configuration
	 * classes, an {@code info} message will be logged.</li>
	 * </ul>
	 * 
	 * @param configAttributes the context configuration attributes to process
	 * @throws IllegalArgumentException if the supplied configuration attributes are
	 * <code>null</code>, or if the supplied configuration attributes include both
	 * resource locations and annotated classes
	 * @throws IllegalStateException if the XML-based loader detects default
	 * configuration classes; if the annotation-based loader detects default
	 * resource locations; if neither candidate loader detects defaults for the supplied
	 * context configuration; or if both candidate loaders detect defaults for the
	 * supplied context configuration
	 */
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {

		Assert.notNull(configAttributes, "configAttributes must not be null");
		Assert.isTrue(!(configAttributes.hasLocations() && configAttributes.hasClasses()), String.format(
			"Cannot process locations AND classes for context "
					+ "configuration %s; configure one or the other, but not both.", configAttributes));

		// If the original locations or classes were not empty, there's no
		// need to bother with default detection checks; just let the
		// appropriate loader process the configuration.
		if (configAttributes.hasLocations()) {
			delegateProcessing(getXmlLoader(), configAttributes);
		} else if (configAttributes.hasClasses()) {
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);
		} else {
			// Else attempt to detect defaults...

			// Let the XML loader process the configuration.
			delegateProcessing(getXmlLoader(), configAttributes);
			boolean xmlLoaderDetectedDefaults = configAttributes.hasLocations();

			if (xmlLoaderDetectedDefaults) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s detected default locations for context configuration %s.",
						name(getXmlLoader()), configAttributes));
				}
			}

			if (configAttributes.hasClasses()) {
				throw new IllegalStateException(String.format(
					"%s should NOT have detected default configuration classes for context configuration %s.",
					name(getXmlLoader()), configAttributes));
			}

			// Now let the annotation config loader process the configuration.
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format(
						"%s detected default configuration classes for context configuration %s.",
						name(getAnnotationConfigLoader()), configAttributes));
				}
			}

			if (!xmlLoaderDetectedDefaults && configAttributes.hasLocations()) {
				throw new IllegalStateException(String.format(
					"%s should NOT have detected default locations for context configuration %s.",
					name(getAnnotationConfigLoader()), configAttributes));
			}

			// If neither loader detected defaults and no initializers were declared,
			// throw an exception.
			if (!configAttributes.hasResources() && ObjectUtils.isEmpty(configAttributes.getInitializers())) {
				throw new IllegalStateException(String.format(
					"Neither %s nor %s was able to detect defaults, and no ApplicationContextInitializers "
							+ "were declared for context configuration %s", name(getXmlLoader()),
					name(getAnnotationConfigLoader()), configAttributes));
			}

			if (configAttributes.hasLocations() && configAttributes.hasClasses()) {
				String message = String.format(
					"Configuration error: both default locations AND default configuration classes "
							+ "were detected for context configuration %s; configure one or the other, but not both.",
					configAttributes);
				logger.error(message);
				throw new IllegalStateException(message);
			}
		}
	}

	/**
	 * Delegates to an appropriate candidate {@code SmartContextLoader} to load
	 * an {@link ApplicationContext}.
	 * 
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders for {@link #getXmlLoader() XML configuration files} and
	 * {@link #getAnnotationConfigLoader() annotated classes}. Specifically, the
	 * delegation algorithm is as follows:
	 * 
	 * <ul>
	 * <li>If the resource locations in the supplied {@code MergedContextConfiguration}
	 * are not empty and the annotated classes are empty,
	 * the XML-based loader will load the {@code ApplicationContext}.</li>
	 * <li>If the annotated classes in the supplied {@code MergedContextConfiguration}
	 * are not empty and the resource locations are empty,
	 * the annotation-based loader will load the {@code ApplicationContext}.</li>
	 * </ul>
	 * 
	 * @param mergedConfig the merged context configuration to use to load the application context
	 * @throws IllegalArgumentException if the supplied merged configuration is <code>null</code>
	 * @throws IllegalStateException if neither candidate loader is capable of loading an
	 * {@code ApplicationContext} from the supplied merged context configuration
	 */
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "mergedConfig must not be null");

		List<SmartContextLoader> candidates = Arrays.asList(getXmlLoader(), getAnnotationConfigLoader());

		for (SmartContextLoader loader : candidates) {
			// Determine if each loader can load a context from the mergedConfig. If it
			// can, let it; otherwise, keep iterating.
			if (supports(loader, mergedConfig)) {
				return delegateLoading(loader, mergedConfig);
			}
		}

		// If neither of the candidates supports the mergedConfig based on resources but
		// ACIs were declared, then delegate to the annotation config loader.
		if (!mergedConfig.getContextInitializerClasses().isEmpty()) {
			return delegateLoading(getAnnotationConfigLoader(), mergedConfig);
		}

		throw new IllegalStateException(String.format(
			"Neither %s nor %s was able to load an ApplicationContext from %s.", name(getXmlLoader()),
			name(getAnnotationConfigLoader()), mergedConfig));
	}

	// --- ContextLoader -------------------------------------------------------

	/**
	 * {@code AbstractDelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#processLocations(Class, String...)} method. Call
	 * {@link #processContextConfiguration(ContextConfigurationAttributes)} instead.
	 * @throws UnsupportedOperationException
	 */
	public final String[] processLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException("DelegatingSmartContextLoaders do not support the ContextLoader SPI. "
				+ "Call processContextConfiguration(ContextConfigurationAttributes) instead.");
	}

	/**
	 * {@code AbstractDelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#loadContext(String...) } method. Call
	 * {@link #loadContext(MergedContextConfiguration)} instead.
	 * @throws UnsupportedOperationException
	 */
	public final ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException("DelegatingSmartContextLoaders do not support the ContextLoader SPI. "
				+ "Call loadContext(MergedContextConfiguration) instead.");
	}

}
