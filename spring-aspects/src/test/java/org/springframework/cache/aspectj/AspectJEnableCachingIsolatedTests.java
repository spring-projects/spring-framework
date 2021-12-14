/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cache.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.NamedCacheResolver;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.testfixture.cache.CacheTestUtils;
import org.springframework.context.testfixture.cache.SomeCustomKeyGenerator;
import org.springframework.context.testfixture.cache.SomeKeyGenerator;
import org.springframework.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import org.springframework.context.testfixture.cache.beans.CacheableService;
import org.springframework.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class AspectJEnableCachingIsolatedTests {

	private ConfigurableApplicationContext ctx;


	private void load(Class<?>... config) {
		this.ctx = new AnnotationConfigApplicationContext(config);
	}

	@AfterEach
	public void closeContext() {
		if (this.ctx != null) {
			this.ctx.close();
		}
	}


	@Test
	public void testKeyStrategy() {
		load(EnableCachingConfig.class);
		AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
		assertThat(aspect.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator", KeyGenerator.class));
	}

	@Test
	public void testCacheErrorHandler() {
		load(EnableCachingConfig.class);
		AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
		assertThat(aspect.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
	}


	// --- local tests -------

	@Test
	public void singleCacheManagerBean() {
		load(SingleCacheManagerConfig.class);
	}

	@Test
	public void multipleCacheManagerBeans() {
		try {
			load(MultiCacheManagerConfig.class);
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("bean of type CacheManager")).isTrue();
		}
	}

	@Test
	public void multipleCacheManagerBeans_implementsCachingConfigurer() {
		load(MultiCacheManagerConfigurer.class); // does not throw
	}

	@Test
	public void multipleCachingConfigurers() {
		try {
			load(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("implementations of CachingConfigurer")).isTrue();
		}
	}

	@Test
	public void noCacheManagerBeans() {
		try {
			load(EmptyConfig.class);
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage().contains("no bean of type CacheManager")).isTrue();
		}
	}

	@Test
	@Disabled("AspectJ has some sort of caching that makes this one fail")
	public void emptyConfigSupport() {
		load(EmptyConfigSupportConfig.class);
		AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
		assertThat(aspect.getCacheResolver()).isNotNull();
		assertThat(aspect.getCacheResolver().getClass()).isEqualTo(SimpleCacheResolver.class);
		assertThat(((SimpleCacheResolver) aspect.getCacheResolver()).getCacheManager()).isSameAs(this.ctx.getBean(CacheManager.class));
	}

	@Test
	public void bothSetOnlyResolverIsUsed() {
		load(FullCachingConfig.class);

		AnnotationCacheAspect aspect = this.ctx.getBean(AnnotationCacheAspect.class);
		assertThat(aspect.getCacheResolver()).isSameAs(this.ctx.getBean("cacheResolver"));
		assertThat(aspect.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator"));
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


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
	static class EmptyConfig {
	}


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
	static class SingleCacheManagerConfig {

		@Bean
		public CacheManager cm1() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
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
	@EnableCaching(mode = AdviceMode.ASPECTJ)
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
	@EnableCaching(mode = AdviceMode.ASPECTJ)
	static class EmptyConfigSupportConfig extends CachingConfigurerSupport {

		@Bean
		public CacheManager cm() {
			return new NoOpCacheManager();
		}

	}


	@Configuration
	@EnableCaching(mode = AdviceMode.ASPECTJ)
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
