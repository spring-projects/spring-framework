/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.aop.support.AopUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link MockitoBean @MockitoBean} used in combination with Spring AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5837">5837</a>
 * @see MockitoSpyBeanAndSpringAopProxyIntegrationTests
 */
@ExtendWith(SpringExtension.class)
class MockitoBeanAndSpringAopProxyIntegrationTests {

	@MockitoBean
	DateService dateService;


	/**
	 * Since the {@code BeanOverrideBeanFactoryPostProcessor} always registers a
	 * manual singleton for a {@code @MockitoBean} mock, the mock that ends up
	 * in the application context should not be proxied by Spring AOP (since
	 * BeanPostProcessors are never applied to manually registered singletons).
	 *
	 * <p>In other words, this test effectively verifies that the mock is a
	 * standard Mockito mock which does <strong>not</strong> have
	 * {@link Cacheable @Cacheable} applied to it.
	 */
	@RepeatedTest(2)
	void mockShouldNotBeAnAopProxy() {
		assertThat(AopUtils.isAopProxy(dateService)).as("is Spring AOP proxy").isFalse();
		assertThat(Mockito.mockingDetails(dateService).isMock()).as("is Mockito mock").isTrue();

		given(dateService.getDate(false)).willReturn(1L);
		Long date = dateService.getDate(false);
		assertThat(date).isOne();

		given(dateService.getDate(false)).willReturn(2L);
		date = dateService.getDate(false);
		assertThat(date).isEqualTo(2L);

		verify(dateService, times(2)).getDate(false);
		verify(dateService, times(2)).getDate(eq(false));
		verify(dateService, times(2)).getDate(anyBoolean());
	}


	@Configuration(proxyBeanMethods = false)
	@EnableCaching(proxyTargetClass = true)
	@Import(DateService.class)
	static class Config {

		@Bean
		CacheResolver cacheResolver(CacheManager cacheManager) {
			return new SimpleCacheResolver(cacheManager);
		}

		@Bean
		ConcurrentMapCacheManager cacheManager() {
			return new ConcurrentMapCacheManager("test");
		}
	}

	static class DateService {

		@Cacheable("test")
		Long getDate(boolean argument) {
			return System.nanoTime();
		}
	}

}
