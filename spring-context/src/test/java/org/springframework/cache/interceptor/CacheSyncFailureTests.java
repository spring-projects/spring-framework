/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.CacheManager;
import org.springframework.cache.CacheTestUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides various failure scenario linked to the use of {@link Cacheable#sync()}.
 *
 * @author Stephane Nicoll
 * @since 4.3
 */
public class CacheSyncFailureTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private ConfigurableApplicationContext context;

	private SimpleService simpleService;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.simpleService = context.getBean(SimpleService.class);
	}

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void unlessSync() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("@Cacheable(sync=true) does not support unless attribute");
		this.simpleService.unlessSync("key");
	}

	@Test
	public void severalCachesSync() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("@Cacheable(sync=true) only allows a single cache");
		this.simpleService.severalCachesSync("key");
	}

	@Test
	public void severalCachesWithResolvedSync() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("@Cacheable(sync=true) only allows a single cache");
		this.simpleService.severalCachesWithResolvedSync("key");
	}

	@Test
	public void syncWithAnotherOperation() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("@Cacheable(sync=true) cannot be combined with other cache operations");
		this.simpleService.syncWithAnotherOperation("key");
	}

	@Test
	public void syncWithTwoGetOperations() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Only one @Cacheable(sync=true) entry is allowed");
		this.simpleService.syncWithTwoGetOperations("key");
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
	static class Config extends CachingConfigurerSupport {

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
