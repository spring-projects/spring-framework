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
