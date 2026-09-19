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

package org.springframework.cache

import java.util.concurrent.CompletableFuture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class KotlinCacheAsyncLookupTests {

	@Test
	suspend fun suspendingFunctionUsesRetrieveForCacheMissAndHit() {
		AnnotationConfigApplicationContext(CacheConfig::class.java, CachedService::class.java).use { context ->
			val service = context.getBean(CachedService::class.java)
			val cache = context.getBean(CacheManager::class.java).getCache("items") as TrackingCache

			assertThat(service.find("item")).isEqualTo("item-1")
			assertThat(service.find("item")).isEqualTo("item-1")
			assertThat(cache.asyncLookups).isEqualTo(2)
			assertThat(cache.blockingLookups).isZero()
		}
	}

	@Test
	fun ordinaryFunctionKeepsSynchronousCacheLookup() {
		AnnotationConfigApplicationContext(CacheConfig::class.java, CachedService::class.java).use { context ->
			val service = context.getBean(CachedService::class.java)
			val cache = context.getBean(CacheManager::class.java).getCache("items") as TrackingCache

			assertThat(service.findBlocking("item")).isEqualTo("item-1")
			assertThat(service.findBlocking("item")).isEqualTo("item-1")
			assertThat(cache.blockingLookups).isEqualTo(2)
			assertThat(cache.asyncLookups).isZero()
		}
	}


	open class CachedService {
		private var invocations = 0

		@Cacheable("items")
		open suspend fun find(id: String): String = "$id-${++invocations}"

		@Cacheable("items")
		open fun findBlocking(id: String): String = "$id-${++invocations}"
	}


	class TrackingCache : ConcurrentMapCache("items") {
		var blockingLookups = 0
		var asyncLookups = 0

		override fun get(key: Any): Cache.ValueWrapper? {
			blockingLookups++
			return super.get(key)
		}

		override fun retrieve(key: Any): CompletableFuture<*>? {
			asyncLookups++
			return super.retrieve(key)
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	class CacheConfig {
		@Bean
		fun cacheManager(): CacheManager = SimpleCacheManager().apply {
			setCaches(listOf(TrackingCache()))
		}
	}

}
