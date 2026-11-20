/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationjsr107

import org.springframework.cache.jcache.JCacheCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.cache.Caching

@Suppress("UsePropertyAccessSyntax")
@Configuration
class CacheConfiguration {

	// tag::snippet[]
	@Bean
	fun jCacheManager(): javax.cache.CacheManager {
		val cachingProvider = Caching.getCachingProvider()
		return cachingProvider.getCacheManager()
	}

	@Bean
	fun cacheManager(jCacheManager: javax.cache.CacheManager): org.springframework.cache.CacheManager {
		return JCacheCacheManager(jCacheManager)
	}
	// end::snippet[]
}