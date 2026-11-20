/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationjdk;

import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CacheConfiguration {

	// tag::snippet[]
	@Bean
	ConcurrentMapCacheFactoryBean defaultCache() {
		ConcurrentMapCacheFactoryBean cache = new ConcurrentMapCacheFactoryBean();
		cache.setName("default");
		return cache;
	}

	@Bean
	ConcurrentMapCacheFactoryBean booksCache() {
		ConcurrentMapCacheFactoryBean cache = new ConcurrentMapCacheFactoryBean();
		cache.setName("books");
		return cache;
	}

	@Bean
	CacheManager cacheManager(ConcurrentMapCache defaultCache, ConcurrentMapCache booksCache) {

		SimpleCacheManager cacheManager = new SimpleCacheManager();
		cacheManager.setCaches(Set.of(defaultCache, booksCache));
		return cacheManager;
	}
	// end::snippet[]
}
