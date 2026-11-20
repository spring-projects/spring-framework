/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CacheProxyFactoryBean}.
 *
 * @author John Blum
 * @author Juergen Hoeller
 */
class CacheProxyFactoryBeanTests {

	@Test
	void configurationClassWithCacheProxyFactoryBean() {
		try (AnnotationConfigApplicationContext applicationContext =
				new AnnotationConfigApplicationContext(CacheProxyFactoryBeanConfiguration.class)) {
			Greeter greeter = applicationContext.getBean("greeter", Greeter.class);
			assertThat(greeter).isNotNull();
			assertThat(greeter.isCacheMiss()).isFalse();
			assertThat(greeter.greet("John")).isEqualTo("Hello John!");
			assertThat(greeter.isCacheMiss()).isTrue();
			assertThat(greeter.greet("Jon")).isEqualTo("Hello Jon!");
			assertThat(greeter.isCacheMiss()).isTrue();
			assertThat(greeter.greet("John")).isEqualTo("Hello John!");
			assertThat(greeter.isCacheMiss()).isFalse();
			assertThat(greeter.greet()).isEqualTo("Hello World!");
			assertThat(greeter.isCacheMiss()).isTrue();
			assertThat(greeter.greet()).isEqualTo("Hello World!");
			assertThat(greeter.isCacheMiss()).isFalse();
		}
	}


	@Configuration
	@EnableCaching
	static class CacheProxyFactoryBeanConfiguration {

		@Bean
		ConcurrentMapCacheManager cacheManager() {
			return new ConcurrentMapCacheManager("Greetings");
		}

		@Bean
		CacheProxyFactoryBean greeter() {
			CacheProxyFactoryBean factoryBean = new CacheProxyFactoryBean();
			factoryBean.setCacheOperationSources(newCacheOperationSource("greet", newCacheOperation("Greetings")));
			factoryBean.setTarget(new SimpleGreeter());
			return factoryBean;
		}

		CacheOperationSource newCacheOperationSource(String methodName, CacheOperation... cacheOperations) {
			NameMatchCacheOperationSource cacheOperationSource = new NameMatchCacheOperationSource();
			cacheOperationSource.addCacheMethod(methodName, Arrays.asList(cacheOperations));
			return cacheOperationSource;
		}

		CacheableOperation newCacheOperation(String cacheName) {
			CacheableOperation.Builder builder = new CacheableOperation.Builder();
			builder.setCacheManager("cacheManager");
			builder.setCacheName(cacheName);
			return builder.build();
		}
	}


	interface Greeter {

		default boolean isCacheHit() {
			return !isCacheMiss();
		}

		boolean isCacheMiss();

		void setCacheMiss();

		default String greet() {
			return greet("World");
		}

		default String greet(String name) {
			setCacheMiss();
			return String.format("Hello %s!", name);
		}
	}


	static class SimpleGreeter implements Greeter {

		private final AtomicBoolean cacheMiss = new AtomicBoolean();

		@Override
		public boolean isCacheMiss() {
			return this.cacheMiss.getAndSet(false);
		}

		@Override
		public void setCacheMiss() {
			this.cacheMiss.set(true);
		}
	}

}
