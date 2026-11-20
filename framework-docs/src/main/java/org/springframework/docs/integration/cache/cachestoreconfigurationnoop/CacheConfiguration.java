/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationnoop;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CacheConfiguration {

	private CacheManager jdkCache() {
		return null;
	}

	private CacheManager gemfireCache() {
		return null;
	}

	// tag::snippet[]
	@Bean
	CacheManager cacheManager(CacheManager jdkCache, CacheManager gemfireCache) {
		CompositeCacheManager cacheManager = new CompositeCacheManager();
		cacheManager.setCacheManagers(List.of(jdkCache, gemfireCache));
		cacheManager.setFallbackToNoOpCache(true);
		return cacheManager;
	}
	// end::snippet[]
}
