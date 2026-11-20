/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.cache;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * General cache-related test utilities.
 *
 * @author Stephane Nicoll
 */
public class CacheTestUtils {

	/**
	 * Create a {@link SimpleCacheManager} with the specified cache(s).
	 * @param cacheNames the names of the caches to create
	 */
	public static CacheManager createSimpleCacheManager(String... cacheNames) {
		SimpleCacheManager result = new SimpleCacheManager();
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : cacheNames) {
			caches.add(new ConcurrentMapCache(cacheName));
		}
		result.setCaches(caches);
		result.afterPropertiesSet();
		return result;
	}


	/**
	 * Assert the following key is not held within the specified cache(s).
	 */
	public static void assertCacheMiss(Object key, Cache... caches) {
		for (Cache cache : caches) {
			assertThat(cache.get(key)).as("No entry in " + cache + " should have been found with key " + key).isNull();
		}
	}

	/**
	 * Assert the following key has a matching value within the specified cache(s).
	 */
	public static void assertCacheHit(Object key, Object value, Cache... caches) {
		for (Cache cache : caches) {
			Cache.ValueWrapper wrapper = cache.get(key);
			assertThat(wrapper).as("An entry in " + cache + " should have been found with key " + key).isNotNull();
			assertThat(wrapper.get()).as("Wrong value in " + cache + " for entry with key " + key).isEqualTo(value);
		}
	}

}
