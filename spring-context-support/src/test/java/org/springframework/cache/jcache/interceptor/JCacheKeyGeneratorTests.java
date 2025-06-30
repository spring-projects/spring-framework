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

package org.springframework.cache.jcache.interceptor;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.config.JCacheConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
class JCacheKeyGeneratorTests {

	private TestKeyGenerator keyGenerator;

	private SimpleService simpleService;

	private Cache cache;

	@BeforeEach
	void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.keyGenerator = context.getBean(TestKeyGenerator.class);
		this.simpleService = context.getBean(SimpleService.class);
		this.cache = context.getBean(CacheManager.class).getCache("test");
		context.close();
	}

	@Test
	void getSimple() {
		this.keyGenerator.expect(1L);
		Object first = this.simpleService.get(1L);
		Object second = this.simpleService.get(1L);
		assertThat(second).isSameAs(first);

		Object key = new SimpleKey(1L);
		assertThat(cache.get(key).get()).isEqualTo(first);
	}

	@Test
	void getFlattenVararg() {
		this.keyGenerator.expect(1L, "foo", "bar");
		Object first = this.simpleService.get(1L, "foo", "bar");
		Object second = this.simpleService.get(1L, "foo", "bar");
		assertThat(second).isSameAs(first);

		Object key = new SimpleKey(1L, "foo", "bar");
		assertThat(cache.get(key).get()).isEqualTo(first);
	}

	@Test
	void getFiltered() {
		this.keyGenerator.expect(1L);
		Object first = this.simpleService.getFiltered(1L, "foo", "bar");
		Object second = this.simpleService.getFiltered(1L, "foo", "bar");
		assertThat(second).isSameAs(first);

		Object key = new SimpleKey(1L);
		assertThat(cache.get(key).get()).isEqualTo(first);
	}


	@Configuration
	@EnableCaching
	static class Config implements JCacheConfigurer {

		@Bean
		@Override
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		@Override
		public KeyGenerator keyGenerator() {
			return new TestKeyGenerator();
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}

	}

	@CacheDefaults(cacheName = "test")
	public static class SimpleService {
		private final AtomicLong counter = new AtomicLong();

		@CacheResult
		public Object get(long id) {
			return counter.getAndIncrement();
		}

		@CacheResult
		public Object get(long id, String... items) {
			return counter.getAndIncrement();
		}

		@CacheResult
		public Object getFiltered(@CacheKey long id, String... items) {
			return counter.getAndIncrement();
		}

	}


	private static class TestKeyGenerator extends SimpleKeyGenerator {

		private Object[] expectedParams;

		private void expect(Object... params) {
			this.expectedParams = params;
		}

		@Override
		public Object generate(Object target, Method method, Object... params) {
			assertThat(params).as("Unexpected parameters").isEqualTo(expectedParams);
			return new SimpleKey(params);
		}
	}

}
