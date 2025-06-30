/*
 * Copyright 2002-present the original author or authors.
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
