package org.springframework.cache.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration tests for {@link CacheProxyFactoryBean}.
 *
 * @author John Blum
 * @see org.junit.Test
 * @see org.springframework.cache.interceptor.CacheProxyFactoryBean
 * @since 5.0.3
 */
@RunWith(MockitoJUnitRunner.class)
public class CacheProxyFactoryBeanTests {

	private ConfigurableApplicationContext applicationContext;

	@Mock
	private CacheInterceptor mockCacheInterceptor;

	@Spy
	private CacheProxyFactoryBean cacheProxyFactoryBean;

	@Before
	@SuppressWarnings("all")
	public void setup() {
		doReturn(this.mockCacheInterceptor).when(this.cacheProxyFactoryBean).getCachingInterceptor();
	}

	@After
	@SuppressWarnings("all")
	public void tearDown() {
		Optional.ofNullable(this.applicationContext).ifPresent(ConfigurableApplicationContext::close);
	}

	private ConfigurableApplicationContext newApplicationContext(Class<?>... annotatedClasses) {
		return new AnnotationConfigApplicationContext(annotatedClasses);
	}

	@Test
	public void cachingConfiguredWithCacheProxyFactoryBeanIsSuccessful() {

		this.applicationContext = newApplicationContext(CacheProxyFactoryBeanConfiguration.class);

		Greeter greeter = this.applicationContext.getBean("greeter", Greeter.class);

		assertNotNull(greeter);
		assertFalse(greeter.isCacheMiss());
		assertEquals("Hello John!", greeter.greet("John"));
		assertTrue(greeter.isCacheMiss());
		assertEquals("Hello Jon!", greeter.greet("Jon"));
		assertTrue(greeter.isCacheMiss());
		assertEquals("Hello John!", greeter.greet("John"));
		assertFalse(greeter.isCacheMiss());
		assertEquals("Hello World!", greeter.greet());
		assertTrue(greeter.isCacheMiss());
		assertEquals("Hello World!", greeter.greet());
		assertFalse(greeter.isCacheMiss());
	}

	@Test
	@SuppressWarnings("all")
	public void setBeanFactoryConfiguresCacheInterceptorAppropriately() {

		BeanFactory mockBeanFactory = mock(BeanFactory.class);

		CacheManager mockCacheManager = mock(CacheManager.class);

		when(mockBeanFactory.getBean(eq("cacheManager"), eq(CacheManager.class))).thenReturn(mockCacheManager);

		this.cacheProxyFactoryBean.setBeanFactory(mockBeanFactory);

		verify(this.mockCacheInterceptor, times(1)).setBeanFactory(eq(mockBeanFactory));
		verify(this.mockCacheInterceptor, times(1)).setCacheManager(eq(mockCacheManager));
	}

	@Test
	public void setCacheOperationSourcesConfiguresCacheInterceptorAppropriately() {

		CacheOperationSource mockCacheOperationSourceOne =
			mock(CacheOperationSource.class, "MockCacheOperationSourceOne");

		CacheOperationSource mockCacheOperationSourceTwo =
			mock(CacheOperationSource.class, "MockCacheOperationSourceTwo");

		this.cacheProxyFactoryBean.setCacheOperationSources(mockCacheOperationSourceOne, mockCacheOperationSourceTwo);

		verify(this.mockCacheInterceptor, times(1))
			.setCacheOperationSources(eq(mockCacheOperationSourceOne), eq(mockCacheOperationSourceTwo));
	}

	@Test
	public void setCacheResolverConfiguresCacheInterceptorCacheResolver() {

		CacheResolver mockCacheResolver = mock(CacheResolver.class);

		this.cacheProxyFactoryBean.setCacheResolver(mockCacheResolver);

		verify(this.mockCacheInterceptor, times(1)).setCacheResolver(eq(mockCacheResolver));
	}

	@Test
	public void setCustomKeyGeneratorConfiguresCacheInterceptorKeyGenerator() {

		KeyGenerator mockKeyGenerator = mock(KeyGenerator.class);

		this.cacheProxyFactoryBean.setKeyGenerator(mockKeyGenerator);

		verify(this.mockCacheInterceptor, times(1)).setKeyGenerator(eq(mockKeyGenerator));
	}

	@Test
	public void setKeyGeneratorWithNullConfiguresCacheInterceptorWithSimpleKeyGenerator() {

		this.cacheProxyFactoryBean.setKeyGenerator(null);

		verify(this.mockCacheInterceptor, times(1))
			.setKeyGenerator(eq(CacheProxyFactoryBean.DEFAULT_KEY_GENERATOR));
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

			factoryBean.setCacheOperationSources(
				newCacheOperationSource("greet", newCacheOperation("Greetings")));

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

		private final AtomicBoolean cacheMiss = new AtomicBoolean(false);

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
