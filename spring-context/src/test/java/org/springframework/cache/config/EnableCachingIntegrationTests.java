package org.springframework.cache.config;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.CacheTestUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.springframework.cache.CacheTestUtils.*;

/**
 * Tests that represent real use cases with advanced configuration
 * @author Stephane Nicoll
 */
public class EnableCachingIntegrationTests {

	@Test
	public void fooServiceWithInterface() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FooConfig.class);
		FooService service = context.getBean(FooService.class);
		fooGetSimple(context, service);
	}

	@Test
	public void fooServiceWithInterfaceCglib() {
		ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(FooConfigCglib.class);
		FooService service = context.getBean(FooService.class);
		fooGetSimple(context, service);
	}

	private void fooGetSimple(ApplicationContext context, FooService service) {
		CacheManager cacheManager = context.getBean(CacheManager.class);

		Cache cache = cacheManager.getCache("testCache");

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = service.getSimple(key);
		assertCacheHit(key, value, cache);
	}

	@Configuration
	static class SharedConfig extends CachingConfigurerSupport {
		@Override
		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache");
		}
	}

	@Configuration
	@Import(SharedConfig.class)
	@EnableCaching
	static class FooConfig {
		@Bean
		public FooService fooService() {
			return new FooServiceImpl();
		}
	}

	@Configuration
	@Import(SharedConfig.class)
	@EnableCaching(proxyTargetClass = true)
	static class FooConfigCglib {
		@Bean
		public FooService fooService() {
			return new FooServiceImpl();
		}
	}

	private static interface FooService {
		public Object getSimple(Object key);
	}

	@CacheConfig(cacheNames = "testCache")
	private static class FooServiceImpl implements FooService {
		private final AtomicLong counter = new AtomicLong();

		@Override
		@Cacheable
		public Object getSimple(Object key) {
			return counter.getAndIncrement();
		}
	}

}
