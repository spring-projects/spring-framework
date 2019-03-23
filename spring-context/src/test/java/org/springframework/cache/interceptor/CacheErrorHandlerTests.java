/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Stephane Nicoll
 */
public class CacheErrorHandlerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Cache cache;

	private CacheInterceptor cacheInterceptor;

	private CacheErrorHandler errorHandler;

	private SimpleService simpleService;

	@Before
	public void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.cache = context.getBean("mockCache", Cache.class);
		this.cacheInterceptor = context.getBean(CacheInterceptor.class);
		this.errorHandler = context.getBean(CacheErrorHandler.class);
		this.simpleService = context.getBean(SimpleService.class);
	}

	@Test
	public void getFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);

		Object result = this.simpleService.get(0L);
		verify(this.errorHandler).handleCacheGetError(exception, cache, 0L);
		verify(this.cache).get(0L);
		verify(this.cache).put(0L, result); // result of the invocation
	}

	@Test
	public void getAndPutFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);
		willThrow(exception).given(this.cache).put(0L, 0L); // Update of the cache will fail as well

		Object counter = this.simpleService.get(0L);

		willReturn(new SimpleValueWrapper(2L)).given(this.cache).get(0L);
		Object counter2 = this.simpleService.get(0L);
		Object counter3 = this.simpleService.get(0L);
		assertNotSame(counter, counter2);
		assertEquals(counter2, counter3);
	}

	@Test
	public void getFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		this.thrown.expect(is(exception));
		this.simpleService.get(0L);
	}

	@Test
	public void putFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		willThrow(exception).given(this.cache).put(0L, 0L);

		this.simpleService.put(0L);
		verify(this.errorHandler).handleCachePutError(exception, cache, 0L, 0L);
	}

	@Test
	public void putFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		willThrow(exception).given(this.cache).put(0L, 0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		this.thrown.expect(is(exception));
		this.simpleService.put(0L);
	}

	@Test
	public void evictFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).evict(0L);

		this.simpleService.evict(0L);
		verify(this.errorHandler).handleCacheEvictError(exception, cache, 0L);
	}

	@Test
	public void evictFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).evict(0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		this.thrown.expect(is(exception));
		this.simpleService.evict(0L);
	}

	@Test
	public void clearFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).clear();

		this.simpleService.clear();
		verify(this.errorHandler).handleCacheClearError(exception, cache);
	}

	@Test
	public void clearFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).clear();

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		this.thrown.expect(is(exception));
		this.simpleService.clear();
	}


	@Configuration
	@EnableCaching
	static class Config extends CachingConfigurerSupport {

		@Bean
		@Override
		public CacheErrorHandler errorHandler() {
			return mock(CacheErrorHandler.class);
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}

		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			cacheManager.setCaches(Collections.singletonList(mockCache()));
			return cacheManager;
		}

		@Bean
		public Cache mockCache() {
			Cache cache = mock(Cache.class);
			given(cache.getName()).willReturn("test");
			return cache;
		}

	}

	@CacheConfig(cacheNames = "test")
	public static class SimpleService {
		private AtomicLong counter = new AtomicLong();

		@Cacheable
		public Object get(long id) {
			return this.counter.getAndIncrement();
		}

		@CachePut
		public Object put(long id) {
			return this.counter.getAndIncrement();
		}

		@CacheEvict
		public void evict(long id) {
		}

		@CacheEvict(allEntries = true)
		public void clear() {
		}
	}

}
