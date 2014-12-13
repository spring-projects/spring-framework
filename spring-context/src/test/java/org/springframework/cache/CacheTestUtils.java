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

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import static org.junit.Assert.*;

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
		List<Cache> caches = new ArrayList<Cache>();
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
			assertNull("No entry in " + cache + " should have been found with key " + key, cache.get(key));
		}
	}

	/**
	 * Assert the following key has a matching value within the specified cache(s).
	 */
	public static void assertCacheHit(Object key, Object value, Cache... caches) {
		for (Cache cache : caches) {
			Cache.ValueWrapper wrapper = cache.get(key);
			assertNotNull("An entry in " + cache + " should have been found with key " + key, wrapper);
			assertEquals("Wrong value in " + cache + " for entry with key " + key, value, wrapper.get());
		}
	}

}
