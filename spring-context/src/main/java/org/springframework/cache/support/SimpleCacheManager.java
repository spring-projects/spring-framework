/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
