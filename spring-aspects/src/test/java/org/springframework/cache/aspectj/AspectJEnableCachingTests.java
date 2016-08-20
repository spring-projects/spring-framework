/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cache.aspectj;

import org.springframework.cache.CacheManager;
import org.springframework.cache.CacheTestUtils;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.config.AbstractCacheAnnotationTests;
import org.springframework.cache.config.AnnotatedClassCacheableService;
import org.springframework.cache.config.CacheableService;
import org.springframework.cache.config.DefaultCacheableService;
import org.springframework.cache.config.SomeCustomKeyGenerator;
import org.springframework.cache.config.SomeKeyGenerator;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Stephane Nicoll
 */
public class AspectJEnableCachingTests extends AbstractCacheAnnotationTests {

	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
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

}
