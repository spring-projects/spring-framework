/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationjsr107;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CacheConfiguration {

	// tag::snippet[]
	@Bean
	javax.cache.CacheManager jCacheManager() {
		CachingProvider cachingProvider = Caching.getCachingProvider();
		return cachingProvider.getCacheManager();
	}

	@Bean
	org.springframework.cache.CacheManager cacheManager(javax.cache.CacheManager jCacheManager) {
		return new JCacheCacheManager(jCacheManager);
	}
	// end::snippet[]
}
