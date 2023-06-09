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

package org.springframework.test.context.cache;

import org.springframework.core.SpringProperties;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.util.StringUtils;

/**
 * Collection of utilities for working with context caching.
 *
 * @author Sam Brannen
 * @since 4.3
 */
public abstract class ContextCacheUtils {

	/**
	 * Retrieve the maximum size of the {@link ContextCache}.
	 * <p>Uses {@link SpringProperties} to retrieve a system property or Spring
	 * property named {@value ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME}.
	 * <p>Defaults to {@value ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}
	 * if no such property has been set or if the property is not an integer.
	 * @return the maximum size of the context cache
	 * @see ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
	 */
	public static int retrieveMaxCacheSize() {
		String propertyName = ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME;
		int defaultValue = ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
		return retrieveProperty(propertyName, defaultValue);
	}

	/**
	 * Retrieve the <em>failure threshold</em> for application context loading.
	 * <p>Uses {@link SpringProperties} to retrieve a system property or Spring
	 * property named {@value CacheAwareContextLoaderDelegate#CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME}.
	 * <p>Defaults to {@value CacheAwareContextLoaderDelegate#DEFAULT_CONTEXT_FAILURE_THRESHOLD}
	 * if no such property has been set or if the property is not an integer.
	 * @return the failure threshold
	 * @since 6.1
	 * @see CacheAwareContextLoaderDelegate#CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME
	 * @see CacheAwareContextLoaderDelegate#DEFAULT_CONTEXT_FAILURE_THRESHOLD
	 */
	public static int retrieveContextFailureThreshold() {
		String propertyName = CacheAwareContextLoaderDelegate.CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME;
		int defaultValue = CacheAwareContextLoaderDelegate.DEFAULT_CONTEXT_FAILURE_THRESHOLD;
		return retrieveProperty(propertyName, defaultValue);
	}

	private static int retrieveProperty(String key, int defaultValue) {
		try {
			String value = SpringProperties.getProperty(key);
			if (StringUtils.hasText(value)) {
				return Integer.parseInt(value.trim());
			}
		}
		catch (Exception ex) {
			// ignore
		}

		// Fallback
		return defaultValue;
	}

}
