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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.events.CacheClearEvent;
import org.springframework.cache.interceptor.events.CacheEvictEvent;
import org.springframework.cache.interceptor.events.CacheOperationEvent;
import org.springframework.cache.interceptor.events.CachePutEvent;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for cache operation events published by the caching infrastructure.
 *
 * @author Anıl Şenocak
 * @since 7.1
 */
class CacheOperationEventTests {

	private AnnotationConfigApplicationContext context;

	private Cache cache;

	private SimpleService simpleService;

	private CacheEventListener listener;

	private Method getMethod;

	private Method putMethod;

	private Method evictMethod;

	private Method clearMethod;

	private Method getFutureMethod;

	@BeforeEach
	void setup() throws Exception {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.cache = context.getBean("mockCache", Cache.class);
		this.simpleService = context.getBean(SimpleService.class);
		this.listener = context.getBean(CacheEventListener.class);
		this.getMethod = SimpleService.class.getMethod("get", long.class);
		this.putMethod = SimpleService.class.getMethod("put", long.class);
		this.evictMethod = SimpleService.class.getMethod("evict", long.class);
		this.clearMethod = SimpleService.class.getMethod("clear");
		this.getFutureMethod = SimpleService.class.getMethod("getFuture", long.class);
	}

	@Test
	void cacheableHit_noEvent() {
		given(this.cache.get(0L)).willReturn(new SimpleValueWrapper(42L));

		this.simpleService.get(0L);

		assertThat(this.listener.events).isEmpty();
	}

	@Test
	void cacheableMiss_publishesPutEvent() {
		given(this.cache.get(0L)).willReturn(null);

		this.simpleService.get(0L);

		assertThat(this.listener.events).hasSize(1);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CachePutEvent.class)
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(this.getMethod))
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("test"))
				.satisfies(event -> assertThat(event.getKey()).isEqualTo(0L));
		assertThat(this.listener.events.get(0))
				.satisfies(event -> assertThat(((CachePutEvent) event).getValue()).isEqualTo(0L));
	}

	@Test
	void cachePut_publishesPutEvent() {
		given(this.cache.get(0L)).willReturn(null);

		this.simpleService.put(0L);

		assertThat(this.listener.events).hasSize(1);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CachePutEvent.class)
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(this.putMethod))
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("test"))
				.satisfies(event -> assertThat(event.getKey()).isEqualTo(0L));
		assertThat(this.listener.events.get(0))
				.satisfies(event -> assertThat(((CachePutEvent) event).getValue()).isEqualTo(0L));
	}

	@Test
	void cacheEvict_publishesEvictEvent() {
		this.simpleService.evict(0L);

		assertThat(this.listener.events).hasSize(1);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CacheEvictEvent.class)
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(this.evictMethod))
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("test"))
				.satisfies(event -> assertThat(event.getKey()).isEqualTo(0L));
	}

	@Test
	void cacheEvictAll_publishesClearEvent() {
		this.simpleService.clear();

		assertThat(this.listener.events).hasSize(1);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CacheClearEvent.class)
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(this.clearMethod))
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("test"))
				.satisfies(event -> assertThat(event.getKey()).isNull());
	}

	@Test
	void cacheableMissWithMultipleCaches_publishesMultiplePutEvents() {
		this.simpleService.getMultiple(0L);

		assertThat(this.listener.events).hasSize(2);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CachePutEvent.class)
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("primary"));
		assertThat(this.listener.events.get(1))
				.isExactlyInstanceOf(CachePutEvent.class)
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("secondary"));
	}

	@Test
	void cacheableFuture_publishesEventOnCompletion() throws Exception {
		given(this.cache.retrieve(eq(0L))).willReturn(null);

		this.simpleService.getFuture(0L).get();

		assertThat(this.listener.events).hasSize(1);
		assertThat(this.listener.events.get(0))
				.isExactlyInstanceOf(CachePutEvent.class)
				.satisfies(event -> assertThat(event.getMethod()).isEqualTo(this.getFutureMethod))
				.satisfies(event -> assertThat(event.getCacheName()).isEqualTo("test"))
				.satisfies(event -> assertThat(event.getKey()).isEqualTo(0L));
	}

	@Configuration
	@EnableCaching
	static class Config {

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}

		@Bean
		public CacheEventListener cacheEventListener() {
			return new CacheEventListener();
		}

		@Bean
		public CacheManager cacheManager(Cache mockCache) {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			Cache primary = createMockCache();
			given(primary.getName()).willReturn("primary");
			Cache secondary = createMockCache();
			given(secondary.getName()).willReturn("secondary");
			cacheManager.setCaches(List.of(mockCache, primary, secondary));
			return cacheManager;
		}

		@Bean
		public Cache mockCache() {
			Cache cache = createMockCache();
			given(cache.getName()).willReturn("test");
			return cache;
		}

		private static Cache createMockCache() {
			return mock(Cache.class);
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

		@Cacheable(cacheNames = {"primary", "secondary"})
		public Object getMultiple(long id) {
			return this.counter.getAndIncrement();
		}

		@Cacheable
		public CompletableFuture<Long> getFuture(long id) {
			return CompletableFuture.completedFuture(this.counter.getAndIncrement());
		}
	}

	public static class CacheEventListener implements ApplicationListener<CacheOperationEvent> {

		public final List<CacheOperationEvent> events = new ArrayList<>();

		@Override
		public void onApplicationEvent(CacheOperationEvent event) {
			this.events.add(event);
		}

	}
}
