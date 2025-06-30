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

package org.springframework.cache.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurer;
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
import org.springframework.context.testfixture.cache.AbstractCacheAnnotationTests;
import org.springframework.context.testfixture.cache.CacheTestUtils;
import org.springframework.context.testfixture.cache.SomeCustomKeyGenerator;
import org.springframework.context.testfixture.cache.SomeKeyGenerator;
import org.springframework.context.testfixture.cache.beans.AnnotatedClassCacheableService;
import org.springframework.context.testfixture.cache.beans.CacheableService;
import org.springframework.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@code @EnableCaching} and its related
 * {@code @Configuration} classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 */
class EnableCachingTests extends AbstractCacheAnnotationTests {

	/** hook into superclass suite of tests */
	@Override
	protected ConfigurableApplicationContext getApplicationContext() {
		return new AnnotationConfigApplicationContext(EnableCachingConfig.class);
	}

	@Test
	void keyStrategy() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertThat(ci.getKeyGenerator()).isSameAs(this.ctx.getBean("keyGenerator", KeyGenerator.class));
	}

	@Test
	void cacheErrorHandler() {
		CacheInterceptor ci = this.ctx.getBean(CacheInterceptor.class);
		assertThat(ci.getErrorHandler()).isSameAs(this.ctx.getBean("errorHandler", CacheErrorHandler.class));
	}

	@Test
	void singleCacheManagerBean() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(SingleCacheManagerConfig.class);
		assertThatCode(ctx::refresh).doesNotThrowAnyException();
		ctx.close();
	}

	@Test
	void multipleCacheManagerBeans() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfig.class);
		assertThatThrownBy(ctx::refresh)
				.isInstanceOfSatisfying(NoUniqueBeanDefinitionException.class, ex -> {
					assertThat(ex.getMessage()).contains(
							"no CacheResolver specified and expected single matching CacheManager but found 2")
							.contains("cm1", "cm2");
					assertThat(ex.getNumberOfBeansFound()).isEqualTo(2);
					assertThat(ex.getBeanNamesFound()).containsExactlyInAnyOrder("cm1", "cm2");
				}).hasNoCause();
	}

	@Test
	void multipleCacheManagerBeans_implementsCachingConfigurer() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class);
		assertThatCode(ctx::refresh).doesNotThrowAnyException();
		ctx.close();
	}

	@Test
	void multipleCachingConfigurers() {
		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(MultiCacheManagerConfigurer.class, EnableCachingConfig.class);
		assertThatThrownBy(ctx::refresh)
				.hasMessageContaining("implementations of CachingConfigurer");
	}

	@Test
	void noCacheManagerBeans() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EmptyConfig.class);
		assertThatThrownBy(ctx::refresh)
				.isInstanceOf(NoSuchBeanDefinitionException.class)
				.hasMessageContaining("no CacheResolver specified")
				.hasMessageContaining(
						"register a CacheManager bean or remove the @EnableCaching annotation from your configuration.")
				.hasNoCause();
	}

	@Test
	void emptyConfigSupport() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfigSupportConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isInstanceOfSatisfying(SimpleCacheResolver.class, cacheResolver ->
				assertThat(cacheResolver.getCacheManager()).isSameAs(context.getBean(CacheManager.class)));
		context.close();
	}

	@Test
	void bothSetOnlyResolverIsUsed() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FullCachingConfig.class);
		CacheInterceptor ci = context.getBean(CacheInterceptor.class);
		assertThat(ci.getCacheResolver()).isSameAs(context.getBean("cacheResolver"));
		assertThat(ci.getKeyGenerator()).isSameAs(context.getBean("keyGenerator"));
		context.close();
	}

	@Test
	void mutableKey() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(EnableCachingConfig.class, ServiceWithMutableKey.class);
		ctx.refresh();

		ServiceWithMutableKey service = ctx.getBean(ServiceWithMutableKey.class);
		String result = service.find(new ArrayList<>(List.of("id")));
		assertThat(service.find(new ArrayList<>(List.of("id")))).isSameAs(result);
		ctx.close();
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig implements CachingConfigurer {

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
	static class MultiCacheManagerConfigurer implements CachingConfigurer {

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
	static class EmptyConfigSupportConfig implements CachingConfigurer {

		@Bean
		public CacheManager cm() {
			return new NoOpCacheManager();
		}
	}


	@Configuration
	@EnableCaching
	static class FullCachingConfig implements CachingConfigurer {

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


	static class ServiceWithMutableKey {

		@Cacheable(value = "testCache", keyGenerator = "customKeyGenerator")
		public String find(Collection<String> id) {
			id.add("other");
			return id.toString();
		}
	}

}
