/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.testfixture.cache.AbstractCacheAnnotationTests;
import org.springframework.context.testfixture.cache.SomeCustomKeyGenerator;
import org.springframework.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import org.springframework.context.testfixture.cache.beans.CacheableService;
import org.springframework.context.testfixture.cache.beans.DefaultCacheableService;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
class JCacheEhCacheAnnotationTests extends AbstractCacheAnnotationTests {

	private final TransactionTemplate txTemplate = new TransactionTemplate(new CallCountingTransactionManager());

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
		return Caching.getCachingProvider("org.ehcache.jsr107.EhcacheCachingProvider");
	}

	@AfterEach
	void shutdown() {
		if (jCacheManager != null) {
			jCacheManager.close();
		}
	}


	@Override
	@Test
	@Disabled("Multi cache manager support to be added")
	protected void customCacheManager() {
	}

	@Test
	void evictWithTransaction() {
		txTemplate.executeWithoutResult(s -> testEvict(this.cs, false));
	}

	@Test
	void evictEarlyWithTransaction() {
		txTemplate.executeWithoutResult(s -> testEvictEarly(this.cs));
	}

	@Test
	void evictAllWithTransaction() {
		txTemplate.executeWithoutResult(s -> testEvictAll(this.cs, false));
	}

	@Test
	void evictAllEarlyWithTransaction() {
		txTemplate.executeWithoutResult(s -> testEvictAllEarly(this.cs));
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig implements CachingConfigurer {

		@Autowired
		CachingProvider cachingProvider;

		@Override
		@Bean
		public org.springframework.cache.CacheManager cacheManager() {
			JCacheCacheManager cm = new JCacheCacheManager(jCacheManager());
			cm.setTransactionAware(true);
			return cm;
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
