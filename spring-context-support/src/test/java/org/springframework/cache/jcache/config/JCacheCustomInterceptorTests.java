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

package org.springframework.cache.jcache.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.jcache.interceptor.AnnotatedJCacheableService;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 *
 * @author Stephane Nicoll
 */
public class JCacheCustomInterceptorTests {

	protected ConfigurableApplicationContext ctx;

	protected JCacheableService<?> cs;

	protected Cache exceptionCache;

	@Before
	public void setup() {
		ctx = new AnnotationConfigApplicationContext(EnableCachingConfig.class);
		cs = ctx.getBean("service", JCacheableService.class);
		exceptionCache = ctx.getBean("exceptionCache", Cache.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void onlyOneInterceptorIsAvailable() {
		Map<String, JCacheInterceptor> interceptors = ctx.getBeansOfType(JCacheInterceptor.class);
		assertEquals("Only one interceptor should be defined", 1, interceptors.size());
		JCacheInterceptor interceptor = interceptors.values().iterator().next();
		assertEquals("Custom interceptor not defined", TestCacheInterceptor.class, interceptor.getClass());
	}

	@Test
	public void customInterceptorAppliesWithRuntimeException() {
		Object o = cs.cacheWithException("id", true);
		assertEquals(55L, o); // See TestCacheInterceptor
	}

	@Test
	public void customInterceptorAppliesWithCheckedException() {
		try {
			cs.cacheWithCheckedException("id", true);
			fail("Should have failed");
		}
		catch (RuntimeException e) {
			assertNotNull("missing original exception", e.getCause());
			assertEquals(IOException.class, e.getCause().getClass());
		}
		catch (Exception e) {
			fail("Wrong exception type " + e);
		}
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig {

		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cm = new SimpleCacheManager();
			cm.setCaches(Arrays.asList(
					defaultCache(),
					exceptionCache()));
			return cm;
		}

		@Bean
		public JCacheableService<?> service() {
			return new AnnotatedJCacheableService(defaultCache());
		}

		@Bean
		public Cache defaultCache() {
			return new ConcurrentMapCache("default");
		}

		@Bean
		public Cache exceptionCache() {
			return new ConcurrentMapCache("exception");
		}

		@Bean
		public JCacheInterceptor jCacheInterceptor(JCacheOperationSource cacheOperationSource) {
			JCacheInterceptor cacheInterceptor = new TestCacheInterceptor();
			cacheInterceptor.setCacheOperationSource(cacheOperationSource);
			return cacheInterceptor;
		}
	}

	/**
	 * A test {@link org.springframework.cache.interceptor.CacheInterceptor} that handles special exception
	 * types.
	 */
	@SuppressWarnings("serial")
	static class TestCacheInterceptor extends JCacheInterceptor {

		@Override
		protected Object invokeOperation(CacheOperationInvoker invoker) {
			try {
				return super.invokeOperation(invoker);
			}
			catch (CacheOperationInvoker.ThrowableWrapper e) {
				Throwable original = e.getOriginal();
				if (original.getClass() == UnsupportedOperationException.class) {
					return 55L;
				}
				else {
					throw new CacheOperationInvoker.ThrowableWrapper(
							new RuntimeException("wrapping original", original));
				}
			}
		}
	}

}
