/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.aspectj;

import java.util.Arrays;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.config.AnnotatedJCacheableService;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.contextsupport.testfixture.jcache.AbstractJCacheAnnotationTests;

/**
 * @author Stephane Nicoll
 */
class JCacheAspectJJavaConfigTests extends AbstractJCacheAnnotationTests {

	@Override
	protected ApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
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
		public AnnotatedJCacheableService cacheableService() {
			return new AnnotatedJCacheableService(defaultCache());
		}

		@Bean
		public Cache defaultCache() {
			return new ConcurrentMapCache("default");
		}
	}

}
