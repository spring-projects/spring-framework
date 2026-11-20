/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.testfixture.cache.CacheTestUtils;
import org.springframework.context.testfixture.cache.beans.CacheableService;
import org.springframework.context.testfixture.cache.beans.DefaultCacheableService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests that use a custom {@link CacheInterceptor}.
 *
 * @author Stephane Nicoll
 */
class CustomInterceptorTests {

	protected ConfigurableApplicationContext ctx;

	protected CacheableService<?> cs;

	@BeforeEach
	void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getBeanFactory().addBeanPostProcessor(
				new CacheInterceptorBeanPostProcessor(context.getBeanFactory()));
		context.register(EnableCachingConfig.class);
		context.refresh();
		this.ctx = context;
		this.cs = ctx.getBean("service", CacheableService.class);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void onlyOneInterceptorIsAvailable() {
		Map<String, CacheInterceptor> interceptors = this.ctx.getBeansOfType(CacheInterceptor.class);
		assertThat(interceptors).as("Only one interceptor should be defined").hasSize(1);
		CacheInterceptor interceptor = interceptors.values().iterator().next();
		assertThat(interceptor).as("Custom interceptor not defined").isInstanceOf(TestCacheInterceptor.class);
	}

	@Test
	void customInterceptorAppliesWithRuntimeException() {
		Object o = this.cs.throwUnchecked(0L);
		// See TestCacheInterceptor
		assertThat(o).isEqualTo(55L);
	}

	@Test
	void customInterceptorAppliesWithCheckedException() {
		assertThatThrownBy(() -> this.cs.throwChecked(0L))
				.isInstanceOf(RuntimeException.class)
				.hasCauseExactlyInstanceOf(IOException.class);
	}


	@Configuration
	@EnableCaching
	static class EnableCachingConfig {

		@Bean
		public CacheManager cacheManager() {
			return CacheTestUtils.createSimpleCacheManager("testCache", "primary", "secondary");
		}

		@Bean
		public CacheableService<?> service() {
			return new DefaultCacheableService();
		}

	}

	static class CacheInterceptorBeanPostProcessor implements BeanPostProcessor {

		private final BeanFactory beanFactory;

		CacheInterceptorBeanPostProcessor(BeanFactory beanFactory) {this.beanFactory = beanFactory;}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (beanName.equals("cacheInterceptor")) {
				CacheInterceptor cacheInterceptor = new TestCacheInterceptor();
				cacheInterceptor.setCacheManager(beanFactory.getBean(CacheManager.class));
				cacheInterceptor.setCacheOperationSource(beanFactory.getBean(CacheOperationSource.class));
				return cacheInterceptor;
			}
			return bean;
		}

	}

	/**
	 * A test {@link CacheInterceptor} that handles special exception types.
	 */
	@SuppressWarnings("serial")
	static class TestCacheInterceptor extends CacheInterceptor {

		@Override
		protected Object invokeOperation(CacheOperationInvoker invoker) {
			try {
				return super.invokeOperation(invoker);
			}
			catch (CacheOperationInvoker.ThrowableWrapper e) {
				Throwable original = e.getOriginal();
				if (original.getClass() == UnsupportedOperationException.class) {
					return 55L;
				}
				else {
					throw new CacheOperationInvoker.ThrowableWrapper(
							new RuntimeException("wrapping original", original));
				}
			}
		}
	}

}
