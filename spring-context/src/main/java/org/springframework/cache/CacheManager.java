/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache;

import java.util.Collection;

/**
 * Spring's central cache manager SPI.
 * Allows for retrieving named {@link Cache} regions.
 *
 * @author Costin Leau
 * @since 3.1
 */
public interface CacheManager {

	/**
	 * Return the cache associated with the given name.
	 * @param name the cache identifier (must not be {@code null})
	 * @return the associated cache, or {@code null} if none found
	 */
	Cache getCache(String name);

	/**
	 * Return a collection of the cache names known by this manager.
	 * @return the names of all caches known by the cache manager
	 */
	Collection<String> getCacheNames();

}
