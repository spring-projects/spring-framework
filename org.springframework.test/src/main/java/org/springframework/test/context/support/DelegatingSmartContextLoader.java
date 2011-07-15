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

	private final List<? extends SmartContextLoader> candidates = Arrays.asList(new GenericXmlContextLoader(),
		new AnnotationConfigContextLoader());


	// --- SmartContextLoader --------------------------------------------------

	/**
	 * TODO Document generatesDefaults() implementation.
	 */
	public boolean generatesDefaults() {
		for (SmartContextLoader loader : candidates) {
			if (loader.generatesDefaults()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * TODO Document emptyResources().
	 */
	private boolean emptyResources(ContextConfigurationAttributes configAttributes) {
		return ObjectUtils.isEmpty(configAttributes.getLocations())
				&& ObjectUtils.isEmpty(configAttributes.getClasses());
	}

	/**
	 * TODO Document processContextConfiguration() implementation.
	 */
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {

		final boolean emptyResources = emptyResources(configAttributes);

		for (SmartContextLoader loader : candidates) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Delegating to %s to process context configuration [%s].",
					loader.getClass().getName(), configAttributes));
			}

			// If the original locations and classes were not empty, there's no
			// need to bother with default generation checks; just let each
			// loader process the configuration.
			if (!emptyResources) {
				loader.processContextConfiguration(configAttributes);
			}
			// Otherwise, if the loader claims to generate defaults, let it
			// process the configuration.
			else if (loader.generatesDefaults()) {
				loader.processContextConfiguration(configAttributes);
				if (!emptyResources(configAttributes) && logger.isInfoEnabled()) {
					logger.info(String.format("SmartContextLoader candidate %s "
							+ "generated defaults for context configuration [%s].", loader, configAttributes));
				}
			}
		}

		// If any loader claims to generate defaults but none actually did,
		// throw an exception.
		if (generatesDefaults() && emptyResources(configAttributes)) {
			throw new IllegalStateException(String.format("None of the SmartContextLoader candidates %s "
					+ "was able to generate defaults for context configuration [%s].", candidates, configAttributes));
		}
	}

	/**
	 * TODO Document supports(MergedContextConfiguration) implementation.
	 */
	public boolean supports(MergedContextConfiguration mergedConfig) {
		Assert.notNull(mergedConfig, "mergedConfig must not be null");

		for (SmartContextLoader loader : candidates) {
			if (loader.supports(mergedConfig)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * TODO Document loadContext(MergedContextConfiguration) implementation.
	 */
	public ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		Assert.notNull(mergedConfig, "mergedConfig must not be null");

		for (SmartContextLoader loader : candidates) {

			// Ask each loader if it can load a context from the mergedConfig.
			// If a loader can, let it; otherwise, continue iterating over all
			// remaining candidates.
			if (loader.supports(mergedConfig)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Delegating to %s to load context from [%s].",
						loader.getClass().getName(), mergedConfig));
				}
				return loader.loadContext(mergedConfig);
			}
		}

		throw new IllegalStateException(String.format("None of the SmartContextLoader candidates %s "
				+ "was able to load an ApplicationContext from [%s].", candidates, mergedConfig));
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
