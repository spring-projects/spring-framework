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
 * A manager for a set of {@link Cache}s.
 *
 * @author Costin Leau
 * @since 3.1
 */
public interface CacheManager {

	/**
	 * Return the cache associated with the given name.
	 * @param name cache identifier (must not be {@code null})
	 * @return the associated cache, or {@code null} if none is found
	 */
	Cache getCache(String name);

	/**
	 * Return a collection of the caches known by this cache manager.
	 * @return names of caches known by the cache manager
	 */
	Collection<String> getCacheNames();

}
