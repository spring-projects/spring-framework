/*
 * Copyright 2002-2023 the original author or authors.
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

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.testfixture.beans.TestBean
import org.springframework.cache.CacheReproTests.*
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class KotlinCacheReproTests {

	@Test
	fun spr14235AdaptsToSuspendingFunction() {
		runBlocking {
			val context = AnnotationConfigApplicationContext(
				Spr14235Config::class.java,
				Spr14235SuspendingService::class.java
			)
			val bean = context.getBean(Spr14235SuspendingService::class.java)
			val cache = context.getBean(CacheManager::class.java).getCache("itemCache")!!
			val tb: TestBean = bean.findById("tb1")
			assertThat(bean.findById("tb1")).isSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb)
			bean.clear()
			val tb2: TestBean = bean.findById("tb1")
			assertThat(tb2).isNotSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb2)
			bean.clear()
			bean.insertItem(tb)
			assertThat(bean.findById("tb1")).isSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb)
			context.close()
		}
	}

	@Test
	fun spr14235AdaptsToSuspendingFunctionWithSync() {
		runBlocking {
			val context = AnnotationConfigApplicationContext(
				Spr14235Config::class.java,
				Spr14235SuspendingServiceSync::class.java
			)
			val bean = context.getBean(Spr14235SuspendingServiceSync::class.java)
			val cache = context.getBean(CacheManager::class.java).getCache("itemCache")!!
			val tb = bean.findById("tb1")
			assertThat(bean.findById("tb1")).isSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb)
			cache.clear()
			val tb2 = bean.findById("tb1")
			assertThat(tb2).isNotSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb2)
			cache.clear()
			bean.insertItem(tb)
			assertThat(bean.findById("tb1")).isSameAs(tb)
			assertThat(cache["tb1"]!!.get()).isSameAs(tb)
			context.close()
		}
	}

	@Test
	fun spr15271FindsOnInterfaceWithInterfaceProxy() {
		val context = AnnotationConfigApplicationContext(Spr15271ConfigA::class.java)
		val bean = context.getBean(Spr15271Interface::class.java)
		val cache = context.getBean(CacheManager::class.java).getCache("itemCache")!!
		val tb = TestBean("tb1")
		bean.insertItem(tb)
		assertThat(bean.findById("tb1").get()).isSameAs(tb)
		assertThat(cache["tb1"]!!.get()).isSameAs(tb)
		context.close()
	}

	@Test
	fun spr15271FindsOnInterfaceWithCglibProxy() {
		val context = AnnotationConfigApplicationContext(Spr15271ConfigB::class.java)
		val bean = context.getBean(Spr15271Interface::class.java)
		val cache = context.getBean(CacheManager::class.java).getCache("itemCache")!!
		val tb = TestBean("tb1")
		bean.insertItem(tb)
		assertThat(bean.findById("tb1").get()).isSameAs(tb)
		assertThat(cache["tb1"]!!.get()).isSameAs(tb)
		context.close()
	}


	open class Spr14235SuspendingService {

		@Cacheable(value = ["itemCache"])
		open suspend fun findById(id: String): TestBean {
			return TestBean(id)
		}

		@CachePut(cacheNames = ["itemCache"], key = "#item.name")
		open suspend fun insertItem(item: TestBean): TestBean {
			return item
		}

		@CacheEvict(cacheNames = ["itemCache"], allEntries = true)
		open suspend fun clear() {
		}
	}


	open class Spr14235SuspendingServiceSync {
		@Cacheable(value = ["itemCache"], sync = true)
		open suspend fun findById(id: String): TestBean {
			return TestBean(id)
		}

		@CachePut(cacheNames = ["itemCache"], key = "#item.name")
		open suspend fun insertItem(item: TestBean): TestBean {
			return item
		}
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching
	class Spr14235Config {
		@Bean
		fun cacheManager(): CacheManager {
			return ConcurrentMapCacheManager()
		}
	}

}
