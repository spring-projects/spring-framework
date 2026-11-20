/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationnoop

import org.springframework.cache.CacheManager
import org.springframework.cache.support.CompositeCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfiguration {

	private fun jdkCache(): CacheManager? {
		return null
	}

	private fun gemfireCache(): CacheManager? {
		return null
	}

	// tag::snippet[]
	@Bean
	fun cacheManager(jdkCache: CacheManager, gemfireCache: CacheManager): CacheManager {
		return CompositeCacheManager().apply {
			setCacheManagers(listOf(jdkCache, gemfireCache))
			setFallbackToNoOpCache(true)
		}
	}
	// end::snippet[]
}