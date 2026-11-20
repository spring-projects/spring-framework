/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cachestoreconfigurationcaffeine

import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class CacheConfiguration {

	// tag::snippet[]
	@Bean
	fun cacheManager(): CacheManager {
		return CaffeineCacheManager()
	}
	// end::snippet[]
}