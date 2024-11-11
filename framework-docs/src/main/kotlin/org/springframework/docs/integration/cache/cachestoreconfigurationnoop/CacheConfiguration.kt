/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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