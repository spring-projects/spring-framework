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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.config.JCacheConfigurer;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
class JCacheErrorHandlerTests {

	private Cache cache;

	private Cache errorCache;

	private CacheErrorHandler errorHandler;

	private SimpleService simpleService;


	@BeforeEach
	void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class);
		this.cache = context.getBean("mockCache", Cache.class);
		this.errorCache = context.getBean("mockErrorCache", Cache.class);
		this.errorHandler = context.getBean(CacheErrorHandler.class);
		this.simpleService = context.getBean(SimpleService.class);
		context.close();
	}


	@Test
	void getFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(this.cache).get(key);

		this.simpleService.get(0L);
		verify(this.errorHandler).handleCacheGetError(exception, this.cache, key);
	}

	@Test
	void getPutNewElementFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		given(this.cache.get(key)).willReturn(null);
		willThrow(exception).given(this.cache).put(key, 0L);

		this.simpleService.get(0L);
		verify(this.errorHandler).handleCachePutError(exception, this.cache, key, 0L);
	}

	@Test
	void getFailPutExceptionFail() {
		UnsupportedOperationException exceptionOnPut = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		given(this.cache.get(key)).willReturn(null);
		willThrow(exceptionOnPut).given(this.errorCache).put(key, SimpleService.TEST_EXCEPTION);

		try {
			this.simpleService.getFail(0L);
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage()).isEqualTo("Test exception");
		}
		verify(this.errorHandler).handleCachePutError(
				exceptionOnPut, this.errorCache, key, SimpleService.TEST_EXCEPTION);
	}

	@Test
	void putFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(this.cache).put(key, 234L);

		this.simpleService.put(0L, 234L);
		verify(this.errorHandler).handleCachePutError(exception, this.cache, key, 234L);
	}

	@Test
	void evictFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		Object key = SimpleKeyGenerator.generateKey(0L);
		willThrow(exception).given(this.cache).evict(key);

		this.simpleService.evict(0L);
		verify(this.errorHandler).handleCacheEvictError(exception, this.cache, key);
	}

	@Test
	void clearFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).clear();

		this.simpleService.clear();
		verify(this.errorHandler).handleCacheClearError(exception, this.cache);
	}


	@Configuration
	@EnableCaching
	static class Config implements JCacheConfigurer {

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
			return mock();
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}

		@Bean
		public Cache mockCache() {
			Cache cache = mock();
			given(cache.getName()).willReturn("test");
			return cache;
		}

		@Bean
		public Cache mockErrorCache() {
			Cache cache = mock();
			given(cache.getName()).willReturn("error");
			return cache;
		}
	}


	@CacheDefaults(cacheName = "test")
	public static class SimpleService {

		private static final IllegalStateException TEST_EXCEPTION = new IllegalStateException("Test exception");

		private final AtomicLong counter = new AtomicLong();

		@CacheResult
		public Object get(long id) {
			return this.counter.getAndIncrement();
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
