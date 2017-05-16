/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.jcache.config;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.config.SomeKeyGenerator;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.cache.jcache.interceptor.AnnotatedJCacheableService;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class JCacheJavaConfigTests extends AbstractJCacheAnnotationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Override
	protected ApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}


	@Test
	public void fullCachingConfig() throws Exception {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(FullCachingConfig.class);

		DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
		assertSame(context.getBean(KeyGenerator.class), cos.getKeyGenerator());
		assertSame(context.getBean("cacheResolver", CacheResolver.class),
				cos.getCacheResolver());
		assertSame(context.getBean("exceptionCacheResolver", CacheResolver.class),
				cos.getExceptionCacheResolver());
		JCacheInterceptor interceptor = context.getBean(JCacheInterceptor.class);
		assertSame(context.getBean("errorHandler", CacheErrorHandler.class), interceptor.getErrorHandler());
	}

	@Test
	public void emptyConfigSupport() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);

		DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
		assertNotNull(cos.getCacheResolver());
		assertEquals(SimpleCacheResolver.class, cos.getCacheResolver().getClass());
		assertSame(context.getBean(CacheManager.class),
				((SimpleCacheResolver) cos.getCacheResolver()).getCacheManager());
		assertNull(cos.getExceptionCacheResolver());
		context.close();
	}

	@Test
	public void bothSetOnlyResolverIsUsed() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(FullCachingConfigSupport.class);

		DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
		assertSame(context.getBean("cacheResolver"), cos.getCacheResolver());
		assertSame(context.getBean("keyGenerator"), cos.getKeyGenerator());
		assertSame(context.getBean("exceptionCacheResolver"), cos.getExceptionCacheResolver());
		context.close();
	}

	@Test
	public void exceptionCacheResolverLazilyRequired() {
		ConfigurableApplicationContext context =
				new AnnotationConfigApplicationContext(NoExceptionCacheResolverConfig.class);

		try {
			DefaultJCacheOperationSource cos = context.getBean(DefaultJCacheOperationSource.class);
			assertSame(context.getBean("cacheResolver"), cos.getCacheResolver());

			JCacheableService<?> service = context.getBean(JCacheableService.class);
			service.cache("id");

			// This call requires the cache manager to be set
			thrown.expect(IllegalStateException.class);
			service.cacheWithException("test", false);
		}
		finally {
			context.close();
		}
	}


	@Configuration
	@EnableCaching
	public static class EnableCachingConfig {

		@Bean
		public CacheManager cacheManager() {
			SimpleCacheManager cm = new SimpleCacheManager();
			cm.setCaches(Arrays.asList(
					defaultCache(),
					new ConcurrentMapCache("primary"),
					new ConcurrentMapCache("secondary"),
					new ConcurrentMapCache("exception")));
			return cm;
		}

		@Bean
		public JCacheableService<?> cacheableService() {
			return new AnnotatedJCacheableService(defaultCache());
		}

		@Bean
		public Cache defaultCache() {
			return new ConcurrentMapCache("default");
		}
	}


	@Configuration
	@EnableCaching
	public static class FullCachingConfig implements JCacheConfigurer {

		@Override
		@Bean
		public CacheManager cacheManager() {
			return new NoOpCacheManager();
		}

		@Override
		@Bean
		public KeyGenerator keyGenerator() {
			return new SimpleKeyGenerator();
		}

		@Override
		@Bean
		public CacheErrorHandler errorHandler() {
			return new SimpleCacheErrorHandler();
		}

		@Override
		@Bean
		public CacheResolver cacheResolver() {
			return new SimpleCacheResolver(cacheManager());
		}

		@Override
		@Bean
		public CacheResolver exceptionCacheResolver() {
			return new SimpleCacheResolver(cacheManager());
		}
	}


	@Configuration
	@EnableCaching
	public static class EmptyConfigSupportConfig extends JCacheConfigurerSupport {
		@Bean
		public CacheManager cm() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class FullCachingConfigSupport extends JCacheConfigurerSupport {

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

		@Override
		@Bean
		public CacheResolver exceptionCacheResolver() {
			return new NamedCacheResolver(cacheManager(), "exception");
		}
	}


	@Configuration
	@EnableCaching
	static class NoExceptionCacheResolverConfig extends JCacheConfigurerSupport {

		@Override
		@Bean
		public CacheResolver cacheResolver() {
			return new NamedCacheResolver(new ConcurrentMapCacheManager(), "default");
		}

		@Bean
		public JCacheableService<?> cacheableService() {
			return new AnnotatedJCacheableService(new ConcurrentMapCache("default"));
		}
	}

}
