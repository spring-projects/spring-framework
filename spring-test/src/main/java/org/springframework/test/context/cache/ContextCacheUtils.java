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

package org.springframework.test.context.cache;

import org.springframework.core.SpringProperties;
import org.springframework.util.StringUtils;

/**
 * Collection of utilities for working with {@link ContextCache ContextCaches}.
 *
 * @author Sam Brannen
 * @since 4.3
 */
public abstract class ContextCacheUtils {

	/**
	 * Retrieve the maximum size of the {@link ContextCache}.
	 * <p>Uses {@link SpringProperties} to retrieve a system property or Spring
	 * property named {@code spring.test.context.cache.maxSize}.
	 * <p>Falls back to the value of the {@link ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}
	 * if no such property has been set or if the property is not an integer.
	 * @return the maximum size of the context cache
	 * @see ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
	 */
	public static int retrieveMaxCacheSize() {
		try {
			String maxSize = SpringProperties.getProperty(ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
			if (StringUtils.hasText(maxSize)) {
				return Integer.parseInt(maxSize.trim());
			}
		}
		catch (Exception ex) {
			// ignore
		}

		// Fallback
		return ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
	}

}
