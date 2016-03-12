package org.springframework.cache.config;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
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
 * Tests that represent real use cases with advanced configuration.
 *
 * @author Stephane Nicoll
 */
public class EnableCachingIntegrationTests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void fooServiceWithInterface() {
		this.context = new AnnotationConfigApplicationContext(FooConfig.class);
		FooService service = this.context.getBean(FooService.class);
		fooGetSimple(service);
	}

	@Test
	public void fooServiceWithInterfaceCglib() {
		this.context = new AnnotationConfigApplicationContext(FooConfigCglib.class);
		FooService service = this.context.getBean(FooService.class);
		fooGetSimple(service);
	}

	private void fooGetSimple(FooService service) {
		Cache cache = getCache();

		Object key = new Object();
		assertCacheMiss(key, cache);

		Object value = service.getSimple(key);
		assertCacheHit(key, value, cache);
	}

	@Test
	public void beanCondition() {
		this.context = new AnnotationConfigApplicationContext(BeanConditionConfig.class);
		Cache cache = getCache();
		FooService service = context.getBean(FooService.class);

		Object key = new Object();
		service.getWithCondition(key);
		assertCacheMiss(key, cache);
	}

	private Cache getCache() {
		return this.context.getBean(CacheManager.class).getCache("testCache");
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

	private interface FooService {
		Object getSimple(Object key);

		Object getWithCondition(Object key);
	}

	@CacheConfig(cacheNames = "testCache")
	private static class FooServiceImpl implements FooService {
		private final AtomicLong counter = new AtomicLong();

		@Override
		@Cacheable
		public Object getSimple(Object key) {
			return counter.getAndIncrement();
		}

		@Override
		@Cacheable(condition = "@bar.enabled")
		public Object getWithCondition(Object key) {
			return counter.getAndIncrement();
		}
	}

	@Configuration
	@Import(FooConfig.class)
	@EnableCaching
	static class BeanConditionConfig {

		@Bean
		public Bar bar() {
			return new Bar(false);
		}

		static class Bar {
			private final boolean enabled;

			public Bar(boolean enabled) {
				this.enabled = enabled;
			}

			public boolean isEnabled() {
				return enabled;
			}
		}
	}

}
