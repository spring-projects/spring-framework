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

package org.springframework.cache.support;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.springframework.cache.CacheTestUtils.*;

/**
 * Tests to support the @{link BeanAwareCacheManager} implementation
 * 
 * @author Phill Escott
 */
public class BeanAwareCacheManagerTests {

	private CacheManager cacheManager;

	private CachingService cachingService;

	@Before
	public void setUp() {
		ApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.cacheManager = context.getBean("cacheManager", CacheManager.class);

		this.cachingService = context.getBean(CachingService.class);
	}

	@Test
	public void noCustomization() {
		Cache cache = this.cacheManager.getCache("beanCache");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = this.cachingService.get(key);
		assertCacheHit(key, value, cache);
	}

	@Configuration
	@EnableCaching
	static class Config extends CachingConfigurerSupport {

		@Value("${spring.cache.test.dynamc-bean-name:dynamic-bean-name}")
		private String dynamicBeanName;

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new BeanAwareCacheManager();
		}

		@Bean
		public Cache beanCache() {
			return new ConcurrentMapCache(this.dynamicBeanName);
		}

		@Bean
		public CachingService cachingService() {
			return new CachingService();
		}
	}

	@CacheConfig(cacheNames = "beanCache")
	static class CachingService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable
		public Object get(Object key) {
			return this.counter.getAndIncrement();
		}
	}
}
