/*
 * Copyright 2002-2022 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Stephane Nicoll
 */
public class CustomInterceptorTests {

	protected ConfigurableApplicationContext ctx;

	protected CacheableService<?> cs;

	@BeforeEach
	public void setup() {
		this.ctx = new AnnotationConfigApplicationContext(EnableCachingConfig.class);
		this.cs = ctx.getBean("service", CacheableService.class);
	}

	@AfterEach
	public void tearDown() {
		this.ctx.close();
	}

	@Test
	public void onlyOneInterceptorIsAvailable() {
		Map<String, CacheInterceptor> interceptors = this.ctx.getBeansOfType(CacheInterceptor.class);
		assertThat(interceptors.size()).as("Only one interceptor should be defined").isEqualTo(1);
		CacheInterceptor interceptor = interceptors.values().iterator().next();
		assertThat(interceptor.getClass()).as("Custom interceptor not defined").isEqualTo(TestCacheInterceptor.class);
	}

	@Test
	public void customInterceptorAppliesWithRuntimeException() {
		Object o = this.cs.throwUnchecked(0L);
		// See TestCacheInterceptor
		assertThat(o).isEqualTo(55L);
	}

	@Test
	public void customInterceptorAppliesWithCheckedException() {
		assertThatRuntimeException()
			.isThrownBy(() -> this.cs.throwChecked(0L))
			.withCauseExactlyInstanceOf(IOException.class);
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

		@Bean
		public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
			CacheInterceptor cacheInterceptor = new TestCacheInterceptor();
			cacheInterceptor.setCacheManager(cacheManager());
			cacheInterceptor.setCacheOperationSources(cacheOperationSource);
			return cacheInterceptor;
		}
	}

	/**
	 * A test {@link CacheInterceptor} that handles special exception
	 * types.
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
