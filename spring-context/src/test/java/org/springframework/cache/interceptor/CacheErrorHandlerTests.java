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

package org.springframework.cache.interceptor;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Stephane Nicoll
 */
class CacheErrorHandlerTests {

	private AnnotationConfigApplicationContext context;

	private Cache cache;

	private CacheInterceptor cacheInterceptor;

	private CacheErrorHandler errorHandler;

	private SimpleService simpleService;

	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.cache = context.getBean("mockCache", Cache.class);
		this.cacheInterceptor = context.getBean(CacheInterceptor.class);
		this.errorHandler = context.getBean(CacheErrorHandler.class);
		this.simpleService = context.getBean(SimpleService.class);
	}

	@AfterEach
	void tearDown() {
		this.context.close();
	}

	@Test
	void getFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);

		Object result = this.simpleService.get(0L);
		verify(this.errorHandler).handleCacheGetError(exception, cache, 0L);
		verify(this.cache).get(0L);
		verify(this.cache).put(0L, result); // result of the invocation
	}

	@Test
	void getAndPutFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);
		willThrow(exception).given(this.cache).put(0L, 0L); // Update of the cache will fail as well

		Object counter = this.simpleService.get(0L);

		willReturn(new SimpleValueWrapper(2L)).given(this.cache).get(0L);
		Object counter2 = this.simpleService.get(0L);
		Object counter3 = this.simpleService.get(0L);
		assertThat(counter2).isNotSameAs(counter);
		assertThat(counter3).isEqualTo(counter2);
	}

	@Test
	void getFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on get");
		willThrow(exception).given(this.cache).get(0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.simpleService.get(0L))
			.withMessage("Test exception on get");
	}

	@Test
	void putFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		willThrow(exception).given(this.cache).put(0L, 0L);

		this.simpleService.put(0L);
		verify(this.errorHandler).handleCachePutError(exception, cache, 0L, 0L);
	}

	@Test
	void putFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on put");
		willThrow(exception).given(this.cache).put(0L, 0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.simpleService.put(0L))
			.withMessage("Test exception on put");
	}

	@Test
	void evictFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).evict(0L);

		this.simpleService.evict(0L);
		verify(this.errorHandler).handleCacheEvictError(exception, cache, 0L);
	}

	@Test
	void evictFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).evict(0L);

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.simpleService.evict(0L))
			.withMessage("Test exception on evict");
	}

	@Test
	void clearFail() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on evict");
		willThrow(exception).given(this.cache).clear();

		this.simpleService.clear();
		verify(this.errorHandler).handleCacheClearError(exception, cache);
	}

	@Test
	void clearFailProperException() {
		UnsupportedOperationException exception = new UnsupportedOperationException("Test exception on clear");
		willThrow(exception).given(this.cache).clear();

		this.cacheInterceptor.setErrorHandler(new SimpleCacheErrorHandler());

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				this.simpleService.clear())
			.withMessage("Test exception on clear");
	}


	@Configuration
	@EnableCaching
	static class Config implements CachingConfigurer {

		@Bean
		@Override
		public CacheErrorHandler errorHandler() {
			return mock();
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}

		@Override
		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			cacheManager.setCaches(Collections.singletonList(mockCache()));
			return cacheManager;
		}

		@Bean
		public Cache mockCache() {
			Cache cache = mock();
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
