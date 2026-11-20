/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.interceptor.SimpleExceptionCacheResolver;
import org.springframework.cache.support.SimpleCacheManager;

/**
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
public abstract class AbstractJCacheTests {

	protected String cacheName;


	@BeforeEach
	void trackCacheName(TestInfo testInfo) {
		this.cacheName = testInfo.getTestMethod().get().getName();
	}


	protected final CacheManager cacheManager = createSimpleCacheManager("default", "simpleCache");

	protected final CacheResolver defaultCacheResolver = new SimpleCacheResolver(cacheManager);

	protected final CacheResolver defaultExceptionCacheResolver = new SimpleExceptionCacheResolver(cacheManager);

	protected final KeyGenerator defaultKeyGenerator = new SimpleKeyGenerator();

	protected static CacheManager createSimpleCacheManager(String... cacheNames) {
		SimpleCacheManager result = new SimpleCacheManager();
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : cacheNames) {
			caches.add(new ConcurrentMapCache(cacheName));
		}
		result.setCaches(caches);
		result.afterPropertiesSet();
		return result;
	}

}
