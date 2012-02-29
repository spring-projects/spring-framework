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
 * {@code DelegatingSmartContextLoader} is an implementation of the {@link SmartContextLoader}
 * SPI that delegates to a set of <em>candidate</em> SmartContextLoaders (i.e.,
 * {@link GenericXmlContextLoader} and {@link AnnotationConfigContextLoader}) to
 * determine which context loader is appropriate for a given test class’s configuration.
 * Each candidate is given a chance to {@link #processContextConfiguration process} the
 * {@link ContextConfigurationAttributes} for each class in the test class hierarchy that
 * is annotated with {@link ContextConfiguration @ContextConfiguration}, and the candidate
 * that supports the merged, processed configuration will be used to actually
 * {@link #loadContext load} the context.
 * 
 * <p>Placing an empty {@code @ContextConfiguration} annotation on a test class signals
 * that default resource locations (i.e., XML configuration files) or default
 * {@link org.springframework.context.annotation.Configuration configuration classes}
 * should be detected. Furthermore, if a specific {@link ContextLoader} or
 * {@link SmartContextLoader} is not explicitly declared via
 * {@code @ContextConfiguration}, {@code DelegatingSmartContextLoader} will be used as
 * the default loader, thus providing automatic support for either XML configuration
 * files or configuration classes, but not both simultaneously.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see GenericXmlContextLoader
 * @see AnnotationConfigContextLoader
 */
public class DelegatingSmartContextLoader implements SmartContextLoader {

	private static final Log logger = LogFactory.getLog(DelegatingSmartContextLoader.class);

	private final SmartContextLoader xmlLoader = new GenericXmlContextLoader();
	private final SmartContextLoader annotationConfigLoader = new AnnotationConfigContextLoader();


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

	private static boolean supports(SmartContextLoader loader, MergedContextConfiguration mergedConfig) {
		if (loader instanceof AnnotationConfigContextLoader) {
			return ObjectUtils.isEmpty(mergedConfig.getLocations()) && !ObjectUtils.isEmpty(mergedConfig.getClasses());
		}
		else {
			return !ObjectUtils.isEmpty(mergedConfig.getLocations()) && ObjectUtils.isEmpty(mergedConfig.getClasses());
		}
	}

	/**
	 * Delegates to candidate {@code SmartContextLoaders} to process the supplied
	 * {@link ContextConfigurationAttributes}.
	 * 
	 * <p>Delegation is based on explicit knowledge of the implementations of
	 * {@link GenericXmlContextLoader} and {@link AnnotationConfigContextLoader}.
	 * Specifically, the delegation algorithm is as follows:
	 * 
	 * <ul>
	 * <li>If the resource locations or configuration classes in the supplied
	 * {@code ContextConfigurationAttributes} are not empty, the appropriate 
	 * candidate loader will be allowed to process the configuration <em>as is</em>,
	 * without any checks for detection of defaults.</li>
	 * <li>Otherwise, {@code GenericXmlContextLoader} will be allowed to process
	 * the configuration in order to detect default resource locations. If
	 * {@code GenericXmlContextLoader} detects default resource locations,
	 * an {@code info} message will be logged.</li>
	 * <li>Subsequently, {@code AnnotationConfigContextLoader} will be allowed to
	 * process the configuration in order to detect default configuration classes.
	 * If {@code AnnotationConfigContextLoader} detects default configuration
	 * classes, an {@code info} message will be logged.</li>
	 * </ul>
	 * 
	 * @param configAttributes the context configuration attributes to process
	 * @throws IllegalArgumentException if the supplied configuration attributes are
	 * <code>null</code>, or if the supplied configuration attributes include both
	 * resource locations and configuration classes
	 * @throws IllegalStateException if {@code GenericXmlContextLoader} detects default
	 * configuration classes; if {@code AnnotationConfigContextLoader} detects default
	 * resource locations; if neither candidate loader detects defaults for the supplied
	 * context configuration; or if both candidate loaders detect defaults for the
	 * supplied context configuration
	 */
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {

		Assert.notNull(configAttributes, "configAttributes must not be null");
		Assert.isTrue(!(configAttributes.hasLocations() && configAttributes.hasClasses()), String.format(
			"Cannot process locations AND configuration classes for context "
					+ "configuration %s; configure one or the other, but not both.", configAttributes));

		// If the original locations or classes were not empty, there's no
		// need to bother with default detection checks; just let the
		// appropriate loader process the configuration.
		if (configAttributes.hasLocations()) {
			delegateProcessing(xmlLoader, configAttributes);
		}
		else if (configAttributes.hasClasses()) {
			delegateProcessing(annotationConfigLoader, configAttributes);
		}
		else {
			// Else attempt to detect defaults...

			// Let the XML loader process the configuration.
			delegateProcessing(xmlLoader, configAttributes);
			boolean xmlLoaderDetectedDefaults = configAttributes.hasLocations();

			if (xmlLoaderDetectedDefaults) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("%s detected default locations for context configuration %s.",
						name(xmlLoader), configAttributes));
				}
			}

			if (configAttributes.hasClasses()) {
				throw new IllegalStateException(String.format(
					"%s should NOT have detected default configuration classes for context configuration %s.",
					name(xmlLoader), configAttributes));
			}

			// Now let the annotation config loader process the configuration.
			delegateProcessing(annotationConfigLoader, configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format(
						"%s detected default configuration classes for context configuration %s.",
						name(annotationConfigLoader), configAttributes));
				}
			}

			if (!xmlLoaderDetectedDefaults && configAttributes.hasLocations()) {
				throw new IllegalStateException(String.format(
					"%s should NOT have detected default locations for context configuration %s.",
					name(annotationConfigLoader), configAttributes));
			}

			// If neither loader detected defaults, throw an exception.
			if (!configAttributes.hasResources()) {
				throw new IllegalStateException(String.format(
					"Neither %s nor %s was able to detect defaults for context configuration %s.", name(xmlLoader),
					name(annotationConfigLoader), configAttributes));
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
	 * <p>Delegation is based on explicit knowledge of the implementations of
	 * {@link GenericXmlContextLoader} and {@link AnnotationConfigContextLoader}.
	 * Specifically, the delegation algorithm is as follows:
	 * 
	 * <ul>
	 * <li>If the resource locations in the supplied {@code MergedContextConfiguration}
	 * are not empty and the configuration classes are empty,
	 * {@code GenericXmlContextLoader} will load the {@code ApplicationContext}.</li>
	 * <li>If the configuration classes in the supplied {@code MergedContextConfiguration}
	 * are not empty and the resource locations are empty,
	 * {@code AnnotationConfigContextLoader} will load the {@code ApplicationContext}.</li>
	 * </ul>
	 * 
	 * @param parentApplicationContext the parent application context. null if no parent is specified.
	 * @param mergedConfig the merged context configuration to use to load the application context
	 * @throws IllegalArgumentException if the supplied merged configuration is <code>null</code>
	 * @throws IllegalStateException if neither candidate loader is capable of loading an
	 * {@code ApplicationContext} from the supplied merged context configuration
	 */
	public ApplicationContext loadContext(ApplicationContext parentApplicationContext, MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "mergedConfig must not be null");

		List<SmartContextLoader> candidates = Arrays.asList(xmlLoader, annotationConfigLoader);

		for (SmartContextLoader loader : candidates) {
			// Determine if each loader can load a context from the
			// mergedConfig. If it can, let it; otherwise, keep iterating.
			if (supports(loader, mergedConfig)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Delegating to %s to load context from %s.", name(loader), mergedConfig));
				}
				return loader.loadContext(parentApplicationContext, mergedConfig);
			}
		}

		throw new IllegalStateException(String.format(
			"Neither %s nor %s was able to load an ApplicationContext from %s.", name(xmlLoader),
			name(annotationConfigLoader), mergedConfig));
	}

	// --- ContextLoader -------------------------------------------------------

	/**
	 * {@code DelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#processLocations(Class, String...)} method. Call
	 * {@link #processContextConfiguration(ContextConfigurationAttributes)} instead.
	 * @throws UnsupportedOperationException
	 */
	public String[] processLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException("DelegatingSmartContextLoader does not support the ContextLoader SPI. "
				+ "Call processContextConfiguration(ContextConfigurationAttributes) instead.");
	}

	/**
	 * {@code DelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#loadContext(ApplicationContext, String...) } method. Call
	 * {@link #loadContext(ApplicationContext, MergedContextConfiguration)} instead.
	 * @throws UnsupportedOperationException
	 */
	public ApplicationContext loadContext(ApplicationContext parentApplicationContext, String... locations) throws Exception {
		throw new UnsupportedOperationException("DelegatingSmartContextLoader does not support the ContextLoader SPI. "
				+ "Call loadContext(MergedContextConfiguration) instead.");
	}

}
