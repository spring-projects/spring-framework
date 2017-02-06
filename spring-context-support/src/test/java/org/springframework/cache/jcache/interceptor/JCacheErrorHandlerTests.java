/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.config.JCacheConfigurerSupport;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Stephane Nicoll
 */
public class JCacheErrorHandlerTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Cache cache;

	private Cache errorCache;

	private CacheErrorHandler errorHandler;

	private SimpleService simpleService;

	@Before
	public void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.cache = context.getBean("mockCache", Cache.class);
		this.errorCache = context.getBean("mockErrorCache", Cache.class);
		this.errorHandler = context.getBean(CacheErrorHandler.class);
		this.simpleService = context.getBean(SimpleService.class);
	}

	@Test
	public void getFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(cache).get(key);

		this.simpleService.get(0L);
		verify(errorHandler).handleCacheGetError(exception, cache, key);
	}

	@Test
	public void getPutNewElementFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		given(this.cache.get(key)).willReturn(null);
		willThrow(exception).given(this.cache).put(key, 0L);

		this.simpleService.get(0L);
		verify(this.errorHandler).handleCachePutError(exception, this.cache, key, 0L);
	}

	@Test
	public void getFailPutExceptionFail() {
		UnsupportedOperationException exceptionOnPut = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		given(this.cache.get(key)).willReturn(null);
		willThrow(exceptionOnPut).given(this.errorCache).put(key,
				SimpleService.TEST_EXCEPTION);

		try {
			this.simpleService.getFail(0L);
		}
		catch (IllegalStateException ex) {
			assertEquals("Test exception", ex.getMessage());
		}
		verify(this.errorHandler).handleCachePutError(exceptionOnPut,
				this.errorCache, key, SimpleService.TEST_EXCEPTION);
	}

	@Test
	public void putFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(cache).put(key, 234L);

		this.simpleService.put(0L, 234L);
		verify(errorHandler).handleCachePutError(exception, cache, key, 234L);
	}

	@Test
	public void evictFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(cache).evict(key);

		this.simpleService.evict(0L);
		verify(errorHandler).handleCacheEvictError(exception, cache, key);
	}

	@Test
	public void clearFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(cache).clear();

		this.simpleService.clear();
		verify(errorHandler).handleCacheClearError(exception, cache);
	}


	@Configuration
	@EnableCaching
	static class Config extends JCacheConfigurerSupport {

		@Bean
		@Override
		public CacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			cacheManager.setCaches(Arrays.asList(mockCache(), mockErrorCache()));
			return cacheManager;
		}

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
		public Cache mockCache() {
			Cache cache = mock(Cache.class);
			given(cache.getName()).willReturn("test");
			return cache;
		}

		@Bean
		public Cache mockErrorCache() {
			Cache cache = mock(Cache.class);
			given(cache.getName()).willReturn("error");
			return cache;
		}

	}

	@CacheDefaults(cacheName = "test")
	public static class SimpleService {

		private static final IllegalStateException TEST_EXCEPTION =
				new IllegalStateException("Test exception");

		private AtomicLong counter = new AtomicLong();

		@CacheResult
		public Object get(long id) {
			return counter.getAndIncrement();
		}

		@CacheResult(exceptionCacheName = "error")
		public Object getFail(long id) {
			throw TEST_EXCEPTION;
		}

		@CachePut
		public void put(long id, @CacheValue Object object) {
		}

		@CacheRemove
		public void evict(long id) {
		}

		@CacheRemoveAll
		public void clear() {
		}
	}
}
