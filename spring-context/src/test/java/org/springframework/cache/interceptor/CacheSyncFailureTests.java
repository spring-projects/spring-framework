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

package org.springframework.cache.interceptor;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.testfixture.cache.CacheTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Provides various failure scenario linked to the use of {@link Cacheable#sync()}.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
class CacheSyncFailureTests {

	private ConfigurableApplicationContext context;

	private SimpleService simpleService;


	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.simpleService = this.context.getBean(SimpleService.class);
	}

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}


	@Test
	void unlessSync() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.unlessSync("key"))
				.withMessageContaining("A sync=true operation does not support the unless attribute");
	}

	@Test
	void severalCachesSync() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.severalCachesSync("key"))
				.withMessageContaining("A sync=true operation is restricted to a single cache");
	}

	@Test
	void severalCachesWithResolvedSync() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.severalCachesWithResolvedSync("key"))
				.withMessageContaining("A sync=true operation is restricted to a single cache");
	}

	@Test
	void syncWithAnotherOperation() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.syncWithAnotherOperation("key"))
				.withMessageContaining("A sync=true operation cannot be combined with other cache operations");
	}

	@Test
	void syncWithTwoGetOperations() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.syncWithTwoGetOperations("key"))
				.withMessageContaining("Only one sync=true operation is allowed");
	}


	static class SimpleService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable(cacheNames = "testCache", sync = true, unless = "#result > 10")
		public Object unlessSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheNames = {"testCache", "anotherTestCache"}, sync = true)
		public Object severalCachesSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "testCacheResolver", sync = true)
		public Object severalCachesWithResolvedSync(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheNames = "testCache", sync = true)
		@CacheEvict(cacheNames = "anotherTestCache", key = "#arg1")
		public Object syncWithAnotherOperation(Object arg1) {
			return this.counter.getAndIncrement();
		}

		@Caching(cacheable = {
				@Cacheable(cacheNames = "testCache", sync = true),
				@Cacheable(cacheNames = "anotherTestCache", sync = true)
		})
		public Object syncWithTwoGetOperations(Object arg1) {
			return this.counter.getAndIncrement();
		}
	}


	@Configuration
	@EnableCaching
	static class Config implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache", "anotherTestCache");
		}

		@Bean
		public CacheResolver testCacheResolver() {
			return new NamedCacheResolver(cacheManager(), "testCache", "anotherTestCache");
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}
	}

}
