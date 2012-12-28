/*
 * Copyright 2010-2011 the original author or authors.
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

package org.springframework.cache.config;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for @EnableCaching and its related @Configuration classes.
 *
 * @author Chris Beams
 */
public class EnableCachingTests extends AbstractAnnotationTests {

	/** hook into superclass suite of tests */
	@Override
	protected ApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}

	@Test
	public void testKeyStrategy() throws Exception {
		CacheInterceptor ci = ctx.getBean(CacheInterceptor.class);
		Assert.assertSame(ctx.getBean(KeyGenerator.class), ci.getKeyGenerator());
	}

	// --- local tests -------

	@Test
	public void singleCacheManagerBean() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SingleCacheManagerConfig.class);
		ctx.refresh();
	}

	@Test(expected=IllegalStateException.class)
	public void multipleCacheManagerBeans() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfig.class);
		try {
			ctx.refresh();
		} catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("beans of type CacheManager"));
			throw root;
		}
	}

	@Test
	public void multipleCacheManagerBeans_implementsCachingConfigurer() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class);
		ctx.refresh(); // does not throw
	}

	@Test(expected=IllegalStateException.class)
	public void multipleCachingConfigurers() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
		try {
			ctx.refresh();
		} catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("implementations of CachingConfigurer"));
			throw root;
		}
	}

	@Test(expected=IllegalStateException.class)
	public void noCacheManagerBeans() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EmptyConfig.class);
		try {
			ctx.refresh();
		} catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("No bean of type CacheManager"));
			throw root;
		}
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig implements CachingConfigurer {
		@Override
		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cm = new SimpleCacheManager();
			cm.setCaches(Arrays.asList(
					new ConcurrentMapCache("default"),
					new ConcurrentMapCache("primary"),
					new ConcurrentMapCache("secondary")));
			return cm;
		}

		@Bean
		public CacheableService<?> service() {
			return new DefaultCacheableService();
		}

		@Bean
		public CacheableService<?> classService() {
			return new AnnotatedClassCacheableService();
		}

		@Override
		@Bean
		public KeyGenerator keyGenerator() {
			return new SomeKeyGenerator();
		}
	}


	@Configuration
	@EnableCaching
	static class EmptyConfig {
	}


	@Configuration
	@EnableCaching
	static class SingleCacheManagerConfig {
		@Bean
		public CacheManager cm1() { return new NoOpCacheManager(); }
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfig {
		@Bean
		public CacheManager cm1() { return new NoOpCacheManager(); }
		@Bean
		public CacheManager cm2() { return new NoOpCacheManager(); }
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfigurer implements CachingConfigurer {
		@Bean
		public CacheManager cm1() { return new NoOpCacheManager(); }
		@Bean
		public CacheManager cm2() { return new NoOpCacheManager(); }

		@Override
		public CacheManager cacheManager() {
			return cm1();
		}
		@Override
		public KeyGenerator keyGenerator() {
			return null;
		}
	}
}
