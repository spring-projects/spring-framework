/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * A basic, no operation {@link CacheManager} implementation suitable
 * for disabling caching, typically used for backing cache declarations
 * without an actual backing store.
 *
 * <p>This implementation will simply accept any items into the cache,
 * not actually storing them.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see NoOpCache
 */
public class NoOpCacheManager implements CacheManager {

	private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>(16);


	@Override
	public @Nullable Cache getCache(String name) {
		return this.cacheMap.computeIfAbsent(name, NoOpCache::new);
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(this.cacheMap.keySet());
	}

	@Override
	public void resetCaches() {
		this.cacheMap.clear();
	}

}
