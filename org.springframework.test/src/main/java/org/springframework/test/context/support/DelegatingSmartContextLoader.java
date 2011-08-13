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
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * TODO Document DelegatingSmartContextLoader.
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
	private final SmartContextLoader annotationLoader = new AnnotationConfigContextLoader();


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
	 * TODO Document processContextConfiguration() implementation.
	 */
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {

		if (configAttributes.hasLocations() && configAttributes.hasClasses()) {
			throw new IllegalStateException(String.format(
				"Cannot process locations AND configuration classes for context "
						+ "configuration %s; configure one or the other, but not both.", configAttributes));
		}

		// If the original locations or classes were not empty, there's no
		// need to bother with default detection checks; just let the respective
		// loader process the configuration.
		if (configAttributes.hasLocations()) {
			delegateProcessing(xmlLoader, configAttributes);
		}
		else if (configAttributes.hasClasses()) {
			delegateProcessing(annotationLoader, configAttributes);
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

			// Now let the annotation loader process the configuration.
			delegateProcessing(annotationLoader, configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format(
						"%s detected default configuration classes for context configuration %s.",
						name(annotationLoader), configAttributes));
				}
			}

			if (!xmlLoaderDetectedDefaults && configAttributes.hasLocations()) {
				throw new IllegalStateException(String.format(
					"%s should NOT have detected default locations for context configuration %s.",
					name(annotationLoader), configAttributes));
			}

			// If neither loader detected defaults, throw an exception.
			if (!configAttributes.hasResources()) {
				throw new IllegalStateException(String.format(
					"Neither %s nor %s was able to detect defaults for context configuration %s.", name(xmlLoader),
					name(annotationLoader), configAttributes));
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
	 * TODO Document loadContext(MergedContextConfiguration) implementation.
	 */
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "mergedConfig must not be null");

		List<SmartContextLoader> candidates = Arrays.asList(xmlLoader, annotationLoader);

		// Determine if each loader can load a context from the mergedConfig. If
		// it can, let it; otherwise, keep iterating.
		for (SmartContextLoader loader : candidates) {
			if (supports(loader, mergedConfig)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Delegating to %s to load context from %s.", name(loader), mergedConfig));
				}
				return loader.loadContext(mergedConfig);
			}
		}

		throw new IllegalStateException(String.format(
			"Neither %s nor %s was able to load an ApplicationContext from %s.", name(xmlLoader),
			name(annotationLoader), mergedConfig));
	}

	// --- ContextLoader -------------------------------------------------------

	/**
	 * {@code DelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#processLocations(Class, String...)} method. Call
	 * {@link #processContextConfiguration(ContextConfigurationAttributes)} instead.
	 * @throws UnsupportedOperationException
	 */
	public String[] processLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException("DelegatingSmartContextLoader does not support the ContextLoader API. "
				+ "Call processContextConfiguration(ContextConfigurationAttributes) instead.");
	}

	/**
	 * {@code DelegatingSmartContextLoader} does not support the
	 * {@link ContextLoader#loadContext(String...) } method. Call
	 * {@link #loadContext(MergedContextConfiguration)} instead.
	 * @throws UnsupportedOperationException
	 */
	public ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException("DelegatingSmartContextLoader does not support the ContextLoader API. "
				+ "Call loadContext(MergedContextConfiguration) instead.");
	}

}
