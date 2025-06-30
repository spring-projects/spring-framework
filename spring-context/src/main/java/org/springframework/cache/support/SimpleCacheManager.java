/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.Cache;

/**
 * Simple cache manager working against a given collection of caches.
 * Useful for testing or simple caching declarations.
 *
 * <p>When using this implementation directly, i.e. not via a regular
 * bean registration, {@link #initializeCaches()} should be invoked
 * to initialize its internal state once the
 * {@linkplain #setCaches(Collection) caches have been provided}.
 *
 * @author Costin Leau
 * @since 3.1
 * @see NoOpCache
 * @see org.springframework.cache.concurrent.ConcurrentMapCache
 */
public class SimpleCacheManager extends AbstractCacheManager {

	private Collection<? extends Cache> caches = Collections.emptySet();


	/**
	 * Specify the collection of Cache instances to use for this CacheManager.
	 * @see #initializeCaches()
	 */
	public void setCaches(Collection<? extends Cache> caches) {
		this.caches = caches;
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		return this.caches;
	}

}
