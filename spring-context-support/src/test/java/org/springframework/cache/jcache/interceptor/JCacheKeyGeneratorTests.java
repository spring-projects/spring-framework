/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.config.JCacheConfigurerSupport;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class JCacheKeyGeneratorTests {

	private TestKeyGenerator keyGenerator;

	private SimpleService simpleService;

	private Cache cache;

	@Before
	public void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.keyGenerator = context.getBean(TestKeyGenerator.class);
		this.simpleService = context.getBean(SimpleService.class);
		this.cache = context.getBean(CacheManager.class).getCache("test");
	}

	@Test
	public void getSimple() {
		this.keyGenerator.expect(1L);
		Object first = this.simpleService.get(1L);
		Object second = this.simpleService.get(1L);
		assertSame(first, second);

		Object key = new SimpleKey(1L);
		assertEquals(first, cache.get(key).get());
	}

	@Test
	public void getFlattenVararg() {
		this.keyGenerator.expect(1L, "foo", "bar");
		Object first = this.simpleService.get(1L, "foo", "bar");
		Object second = this.simpleService.get(1L, "foo", "bar");
		assertSame(first, second);

		Object key = new SimpleKey(1L, "foo", "bar");
		assertEquals(first, cache.get(key).get());
	}

	@Test
	public void getFiltered() {
		this.keyGenerator.expect(1L);
		Object first = this.simpleService.getFiltered(1L, "foo", "bar");
		Object second = this.simpleService.getFiltered(1L, "foo", "bar");
		assertSame(first, second);

		Object key = new SimpleKey(1L);
		assertEquals(first, cache.get(key).get());
	}


	@Configuration
	@EnableCaching
	static class Config extends JCacheConfigurerSupport {

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
		private AtomicLong counter = new AtomicLong();

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
			assertTrue("Unexpected parameters: expected: "
							+ Arrays.toString(this.expectedParams) + " but got: " + Arrays.toString(params),
					Arrays.equals(expectedParams, params));
			return new SimpleKey(params);
		}
	}
}
