/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationjdk

import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class CacheConfiguration {

	// tag::snippet[]
	@Bean
	fun defaultCache(): ConcurrentMapCacheFactoryBean {
		return ConcurrentMapCacheFactoryBean().apply {
			setName("default")
		}
	}

	@Bean
	fun booksCache(): ConcurrentMapCacheFactoryBean {
		return ConcurrentMapCacheFactoryBean().apply {
			setName("books")
		}
	}

	@Bean
	fun cacheManager(defaultCache: ConcurrentMapCache, booksCache: ConcurrentMapCache): CacheManager {
		return SimpleCacheManager().apply {
			setCaches(setOf(defaultCache, booksCache))
		}
	}
	// end::snippet[]
}