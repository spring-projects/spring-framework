/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.cache.cacheannotationenable

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// tag::snippet[]
@Configuration
@EnableCaching
class CacheConfiguration {

	@Bean
	fun cacheManager(): CacheManager {
		return CaffeineCacheManager().apply {
			setCacheSpecification("...")
		}
	}
}
// end::snippet[]