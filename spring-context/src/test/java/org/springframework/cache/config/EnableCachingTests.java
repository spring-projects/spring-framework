/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.CacheTestUtils;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * Integration tests for {@code @EnableCaching} and its related
 * {@code @Configuration} classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 */
public class EnableCachingTests extends AbstractCacheAnnotationTests {

	/** hook into superclass suite of tests */
	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}

	@Test
	public void testKeyStrategy() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertSame(this.ctx.getBean("keyGenerator", KeyGenerator.class), ci.getKeyGenerator());
	}

	@Test
	public void testCacheErrorHandler() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertSame(this.ctx.getBean("errorHandler", CacheErrorHandler.class), ci.getErrorHandler());
	}

	@Test
	public void singleCacheManagerBean() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SingleCacheManagerConfig.class);
		ctx.refresh();
	}

	@Test(expected = IllegalStateException.class)
	public void multipleCacheManagerBeans() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfig.class);
		try {
			ctx.refresh();
		}
		catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("beans of type CacheManager"));
			throw root;
		}
	}

	@Test
	public void multipleCacheManagerBeans_implementsCachingConfigurer() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class);
		ctx.refresh();  // does not throw an exception
	}

	@Test(expected = IllegalStateException.class)
	public void multipleCachingConfigurers() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
		try {
			ctx.refresh();
		}
		catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("implementations of CachingConfigurer"));
			throw root;
		}
	}

	@Test(expected = IllegalStateException.class)
	public void noCacheManagerBeans() throws Throwable {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EmptyConfig.class);
		try {
			ctx.refresh();
		}
		catch (BeanCreationException ex) {
			Throwable root = ex.getRootCause();
			assertTrue(root.getMessage().contains("No bean of type CacheManager"));
			throw root;
		}
	}

	@Test
	public void emptyConfigSupport() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertNotNull(ci.getCacheResolver());
		assertEquals(SimpleCacheResolver.class, ci.getCacheResolver().getClass());
		assertSame(context.getBean(CacheManager.class), ((SimpleCacheResolver)ci.getCacheResolver()).getCacheManager());
		context.close();
	}

	@Test
	public void bothSetOnlyResolverIsUsed() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FullCachingConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertSame(context.getBean("cacheResolver"), ci.getCacheResolver());
		assertSame(context.getBean("keyGenerator"), ci.getKeyGenerator());
		context.close();
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig extends CachingConfigurerSupport {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache", "primary", "secondary");
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

		@Override
		@Bean
		public CacheErrorHandler errorHandler() {
			return new SimpleCacheErrorHandler();
		}

		@Bean
		public KeyGenerator customKeyGenerator() {
			return new SomeCustomKeyGenerator();
		}

		@Bean
		public CacheManager customCacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache");
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
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfig {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}

		@Bean
		public CacheManager cm2() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class MultiCacheManagerConfigurer extends CachingConfigurerSupport {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}

		@Bean
		public CacheManager cm2() {
			return new NoOpCacheManager();
		}

		@Override
		public CacheManager cacheManager() {
			return cm1();
		}

		@Override
		public KeyGenerator keyGenerator() {
			return null;
		}
	}


	@Configuration
	@EnableCaching
	static class EmptyConfigSupportConfig extends CachingConfigurerSupport {

		@Bean
		public CacheManager cm() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class FullCachingConfig extends CachingConfigurerSupport {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new NoOpCacheManager();
		}

		@Override
		@Bean
		public KeyGenerator keyGenerator() {
			return new SomeKeyGenerator();
		}

		@Override
		@Bean
		public CacheResolver cacheResolver() {
			return new NamedCacheResolver(cacheManager(), "foo");
		}
	}

}
