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
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.testfixture.cache.CacheTestUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.context.testfixture.cache.CacheTestUtils.assertCacheHit;
import static org.springframework.context.testfixture.cache.CacheTestUtils.assertCacheMiss;

/**
 * Provides various {@link CacheResolver} customisations scenario
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
class CacheResolverCustomizationTests {

	private ConfigurableApplicationContext context;

	private CacheManager cacheManager;

	private CacheManager anotherCacheManager;

	private SimpleService simpleService;


	@BeforeEach
	void setup() {
		this.context = new AnnotationConfigApplicationContext(Config.class);
		this.cacheManager = context.getBean("cacheManager", CacheManager.class);
		this.anotherCacheManager = context.getBean("anotherCacheManager", CacheManager.class);
		this.simpleService = context.getBean(SimpleService.class);
	}

	@AfterEach
	void closeContext() {
		this.context.close();
	}


	@Test
	void noCustomization() {
		Cache cache = this.cacheManager.getCache("default");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = this.simpleService.getSimple(key);
		assertCacheHit(key, value, cache);
	}

	@Test
	void customCacheResolver() {
		Cache cache = this.cacheManager.getCache("primary");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = this.simpleService.getWithCustomCacheResolver(key);
		assertCacheHit(key, value, cache);
	}

	@Test
	void customCacheManager() {
		Cache cache = this.anotherCacheManager.getCache("default");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = this.simpleService.getWithCustomCacheManager(key);
		assertCacheHit(key, value, cache);
	}

	@Test
	void runtimeResolution() {
		Cache defaultCache = this.cacheManager.getCache("default");
		Cache primaryCache = this.cacheManager.getCache("primary");

		Object key = new Object();
		assertCacheMiss(key, defaultCache, primaryCache);
		Object value = this.simpleService.getWithRuntimeCacheResolution(key, "default");
		assertCacheHit(key, value, defaultCache);
		assertCacheMiss(key, primaryCache);

		Object key2 = new Object();
		assertCacheMiss(key2, defaultCache, primaryCache);
		Object value2 = this.simpleService.getWithRuntimeCacheResolution(key2, "primary");
		assertCacheHit(key2, value2, primaryCache);
		assertCacheMiss(key2, defaultCache);
	}

	@Test
	void namedResolution() {
		Cache cache = this.cacheManager.getCache("secondary");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = this.simpleService.getWithNamedCacheResolution(key);
		assertCacheHit(key, value, cache);
	}

	@Test
	void noCacheResolved() {
		Method method = ReflectionUtils.findMethod(SimpleService.class, "noCacheResolved", Object.class);

		assertThatIllegalStateException()
				.isThrownBy(() -> this.simpleService.noCacheResolved(new Object()))
				.withMessageContaining(method.toString());
	}

	@Test
	void unknownCacheResolver() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.simpleService.unknownCacheResolver(new Object()))
				.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("unknownCacheResolver"));
	}


	@Configuration
	@EnableCaching
	static class Config implements CachingConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("default", "primary", "secondary");
		}

		@Bean
		public CacheManager anotherCacheManager() {
			return CacheTestUtils.createSimpleCacheManager("default", "primary", "secondary");
		}

		@Bean
		public CacheResolver primaryCacheResolver() {
			return new NamedCacheResolver(cacheManager(), "primary");
		}

		@Bean
		public CacheResolver secondaryCacheResolver() {
			return new NamedCacheResolver(cacheManager(), "primary");
		}

		@Bean
		public CacheResolver runtimeCacheResolver() {
			return new RuntimeCacheResolver(cacheManager());
		}

		@Bean
		public CacheResolver namedCacheResolver() {
			NamedCacheResolver resolver = new NamedCacheResolver();
			resolver.setCacheManager(cacheManager());
			resolver.setCacheNames(Collections.singleton("secondary"));
			return resolver;
		}

		@Bean
		public CacheResolver nullCacheResolver() {
			return new NullCacheResolver(cacheManager());
		}

		@Bean
		public SimpleService simpleService() {
			return new SimpleService();
		}
	}


	@CacheConfig(cacheNames = "default")
	static class SimpleService {

		private final AtomicLong counter = new AtomicLong();

		@Cacheable
		public Object getSimple(Object key) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "primaryCacheResolver")
		public Object getWithCustomCacheResolver(Object key) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheManager = "anotherCacheManager")
		public Object getWithCustomCacheManager(Object key) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "runtimeCacheResolver", key = "#p0")
		public Object getWithRuntimeCacheResolution(Object key, String cacheName) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "namedCacheResolver")
		public Object getWithNamedCacheResolution(Object key) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "nullCacheResolver") // No cache resolved for the operation
		public Object noCacheResolved(Object key) {
			return this.counter.getAndIncrement();
		}

		@Cacheable(cacheResolver = "unknownCacheResolver") // No such bean defined
		public Object unknownCacheResolver(Object key) {
			return this.counter.getAndIncrement();
		}
	}


	/**
	 * Example of {@link CacheResolver} that resolve the caches at
	 * runtime (i.e. based on method invocation parameters).
	 * <p>Expects the second argument to hold the name of the cache to use
	 */
	private static class RuntimeCacheResolver extends AbstractCacheResolver {

		private RuntimeCacheResolver(CacheManager cacheManager) {
			super(cacheManager);
		}

		@Override
		protected @Nullable Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
			String cacheName = (String) context.getArgs()[1];
			return Collections.singleton(cacheName);
		}
	}


	private static class NullCacheResolver extends AbstractCacheResolver {

		private NullCacheResolver(CacheManager cacheManager) {
			super(cacheManager);
		}

		@Override
		protected @Nullable Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
			return null;
		}
	}

}
