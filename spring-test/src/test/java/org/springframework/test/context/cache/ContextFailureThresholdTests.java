/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.cache;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.springframework.test.context.CacheAwareContextLoaderDelegate.CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME;
import static org.springframework.test.context.CacheAwareContextLoaderDelegate.DEFAULT_CONTEXT_FAILURE_THRESHOLD;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * Integration tests for context failure threshold support.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class ContextFailureThresholdTests {

	private static final AtomicInteger loadCount = new AtomicInteger(0);


	@BeforeEach
	@AfterEach
	void resetFlag() {
		loadCount.set(0);
		SpringProperties.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, null);
	}

	@Test
	void defaultThreshold() {
		assertThat(loadCount.get()).isZero();

		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(PassingTestCase.class))// 2 passing
				.selectors(selectClass(FailingTestCase.class))// 3 failing
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(5).succeeded(2).failed(3));
		assertThat(loadCount.get()).isEqualTo(DEFAULT_CONTEXT_FAILURE_THRESHOLD);
	}

	@Test
	void customThreshold() {
		assertThat(loadCount.get()).isZero();

		int threshold = 2;
		SpringProperties.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, Integer.toString(threshold));

		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(PassingTestCase.class))// 2 passing
				.selectors(selectClass(FailingTestCase.class))// 3 failing
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(5).succeeded(2).failed(3));
		assertThat(loadCount.get()).isEqualTo(threshold);
	}

	@Test
	void thresholdEffectivelyDisabled() {
		assertThat(loadCount.get()).isZero();

		SpringProperties.setProperty(CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME, "999999");

		EngineTestKit.engine("junit-jupiter")//
				.selectors(selectClass(PassingTestCase.class))// 2 passing
				.selectors(selectClass(FailingTestCase.class))// 3 failing
				.execute()//
				.testEvents()//
				.assertStatistics(stats -> stats.started(5).succeeded(2).failed(3));
		assertThat(loadCount.get()).isEqualTo(3);
	}


	@SpringJUnitConfig
	@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
	static class PassingTestCase {

		@BeforeAll
		static void verifyInitialCacheState() {
			resetContextCache();
			assertContextCacheStatistics("BeforeAll", 0, 0, 0);
		}

		@AfterAll
		static void verifyFinalCacheState() {
			assertContextCacheStatistics("AfterAll", 1, 1, 1);
			resetContextCache();
		}

		@Test
		void test1() {}

		@Test
		void test2() {}

		@Configuration
		static class PassingConfig {
		}
	}

	@SpringJUnitConfig
	@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
	@TestMethodOrder(OrderAnnotation.class)
	static class FailingTestCase {

		@BeforeAll
		static void verifyInitialCacheState() {
			resetContextCache();
			assertContextCacheStatistics("BeforeAll", 0, 0, 0);
		}

		@AfterAll
		static void verifyFinalCacheState() {
			assertContextCacheStatistics("AfterAll", 0, 0, 3);
			resetContextCache();
		}

		@Test
		void test1() {}

		@Test
		void test2() {}

		@Test
		void test3() {}

		@Configuration
		static class FailingConfig {

			FailingConfig() {
				loadCount.incrementAndGet();
			}

			@Bean
			String explosiveString() {
				throw new RuntimeException("Boom!");
			}
		}
	}

}
