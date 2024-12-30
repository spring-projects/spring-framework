/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AopTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Tests for {@link MockitoSpyBean @MockitoSpyBean} used in combination with Spring AOP.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5837">5837</a>
 * @see MockitoBeanAndSpringAopProxyIntegrationTests
 */
@ExtendWith(SpringExtension.class)
class MockitoSpyBeanAndSpringAopProxyIntegrationTests {

	@MockitoSpyBean
	DateService dateService;


	@BeforeEach
	void resetCache() {
		// We have to clear the "test" cache before each test. Otherwise, method
		// invocations on the Spring AOP proxy will never make it to the Mockito spy.
		dateService.clearCache();
	}

	/**
	 * Stubbing and verification for a Mockito spy that is wrapped in a Spring AOP
	 * proxy should always work when performed via the ultimate target of the Spring
	 * AOP proxy (i.e., the actual spy instance).
	 */
	// We need to run this test at least twice to ensure the Mockito spy can be reused
	// across test method invocations without using @DirtestContext.
	@RepeatedTest(2)
	void stubAndVerifyOnUltimateTargetOfSpringAopProxy() {
		assertThat(AopUtils.isAopProxy(dateService)).as("is Spring AOP proxy").isTrue();
		DateService spy = AopTestUtils.getUltimateTargetObject(dateService);
		assertIsSpy(dateService, "ultimate target");

		given(spy.getDate(false)).willReturn(1L);
		Long date = dateService.getDate(false);
		assertThat(date).isOne();

		given(spy.getDate(false)).willReturn(2L);
		date = dateService.getDate(false);
		assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

		// Each of the following verifies times(1), because the AOP proxy caches the
		// original value and does not delegate to the spy on subsequent invocations.
		verify(spy, times(1)).getDate(false);
		verify(spy, times(1)).getDate(eq(false));
		verify(spy, times(1)).getDate(anyBoolean());
	}

	/**
	 * Verification for a Mockito spy that is wrapped in a Spring AOP proxy should
	 * always work when performed via the Spring AOP proxy. However, stubbing
	 * does not currently work via the Spring AOP proxy.
	 *
	 * <p>Consequently, this test method supplies the ultimate target of the Spring
	 * AOP proxy to stubbing calls, while supplying the Spring AOP proxy to verification
	 * calls.
	 */
	// We need to run this test at least twice to ensure the Mockito spy can be reused
	// across test method invocations without using @DirtestContext.
	@RepeatedTest(2)
	void stubOnUltimateTargetAndVerifyOnSpringAopProxy() {
		assertThat(AopUtils.isAopProxy(dateService)).as("is Spring AOP proxy").isTrue();
		assertIsSpy(dateService, "Spring AOP proxy");

		DateService spy = AopTestUtils.getUltimateTargetObject(dateService);
		given(spy.getDate(false)).willReturn(1L);
		Long date = dateService.getDate(false);
		assertThat(date).isOne();

		given(spy.getDate(false)).willReturn(2L);
		date = dateService.getDate(false);
		assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

		// Each of the following verifies times(1), because the AOP proxy caches the
		// original value and does not delegate to the spy on subsequent invocations.
		verify(dateService, times(1)).getDate(false);
		verify(dateService, times(1)).getDate(eq(false));
		verify(dateService, times(1)).getDate(anyBoolean());
	}

	/**
	 * Ideally, both stubbing and verification should work transparently when a Mockito
	 * spy is wrapped in a Spring AOP proxy. However, Mockito currently does not provide
	 * support for transparent stubbing of a proxied spy. For example, implementing a
	 * custom {@link org.mockito.plugins.MockResolver} will not result in successful
	 * stubbing for a proxied mock.
	 */
	@Disabled("Disabled until Mockito provides support for transparent stubbing of a proxied spy")
	// We need to run this test at least twice to ensure the Mockito spy can be reused
	// across test method invocations without using @DirtestContext.
	@RepeatedTest(2)
	void stubAndVerifyDirectlyOnSpringAopProxy() throws Exception {
		assertThat(AopUtils.isCglibProxy(dateService)).as("is Spring AOP CGLIB proxy").isTrue();
		assertIsSpy(dateService);

		doReturn(1L).when(dateService).getDate(false);
		Long date = dateService.getDate(false);
		assertThat(date).isOne();

		doReturn(2L).when(dateService).getDate(false);
		date = dateService.getDate(false);
		assertThat(date).isEqualTo(1L); // 1L instead of 2L, because the AOP proxy caches the original value.

		// Each of the following verifies times(1), because the AOP proxy caches the
		// original value and does not delegate to the spy on subsequent invocations.
		verify(dateService, times(1)).getDate(false);
		verify(dateService, times(1)).getDate(eq(false));
		verify(dateService, times(1)).getDate(anyBoolean());
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

		@CacheEvict(cacheNames = "test", allEntries = true)
		void clearCache() {
		}

	}

}
