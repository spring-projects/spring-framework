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

package org.springframework.cache.jcache;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.config.AbstractCacheAnnotationTests;
import org.springframework.cache.config.AnnotatedClassCacheableService;
import org.springframework.cache.config.CacheableService;
import org.springframework.cache.config.DefaultCacheableService;
import org.springframework.cache.config.SomeCustomKeyGenerator;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Stephane Nicoll
 */
public class JCacheEhCacheAnnotationTests extends AbstractCacheAnnotationTests {

	private CacheManager jCacheManager;


	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getBeanFactory().registerSingleton("cachingProvider", getCachingProvider());
		context.register(EnableCachingConfig.class);
		context.refresh();
		jCacheManager = context.getBean("jCacheManager", CacheManager.class);
		return context;
	}

	protected CachingProvider getCachingProvider() {
		return Caching.getCachingProvider();
	}

	@After
	public void shutdown() {
		if (jCacheManager != null) {
			jCacheManager.close();
		}
	}


	@Override
	@Test
	@Ignore("Multi cache manager support to be added")
	public void testCustomCacheManager() {
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig extends CachingConfigurerSupport {

		@Autowired
		CachingProvider cachingProvider;

		@Override
		@Bean
		public org.springframework.cache.CacheManager cacheManager() {
			return new JCacheCacheManager(jCacheManager());
		}

		@Bean
		public CacheManager jCacheManager() {
			CacheManager cacheManager = this.cachingProvider.getCacheManager();
			MutableConfiguration<Object, Object> mutableConfiguration = new MutableConfiguration<>();
			mutableConfiguration.setStoreByValue(false);  // otherwise value has to be Serializable
			cacheManager.createCache("testCache", mutableConfiguration);
			cacheManager.createCache("primary", mutableConfiguration);
			cacheManager.createCache("secondary", mutableConfiguration);
			return cacheManager;
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
			return new SimpleKeyGenerator();
		}

		@Bean
		public KeyGenerator customKeyGenerator() {
			return new SomeCustomKeyGenerator();
		}
	}

}
