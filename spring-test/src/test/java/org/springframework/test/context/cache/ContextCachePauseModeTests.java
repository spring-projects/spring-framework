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

package org.springframework.test.context.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextTestUtils;
import org.springframework.test.context.cache.ContextCache.PauseMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.cache.ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;

/**
 * Integration tests for verifying proper behavior of the {@link ContextCache} in
 * conjunction with {@link PauseMode}.
 *
 * @author Sam Brannen
 * @since 7.0.3
 * @see ContextCacheTests
 * @see UnusedContextsIntegrationTests
 */
class ContextCachePauseModeTests {

	private ContextCache contextCache;


	@BeforeEach
	@AfterEach
	void clearApplicationEvents() {
		EventTracker.events.clear();
	}

	@Test
	void topLevelTestClassesWithPauseModeAlways() {
		this.contextCache = new DefaultContextCache(DEFAULT_MAX_CONTEXT_CACHE_SIZE, PauseMode.ALWAYS);

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 0, 1);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 1, 1);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 1, 1, 2, 1);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 3, 1);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 3, 2);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase2", "ContextPaused:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 2, 1, 4, 2);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 2, 1, 5, 2);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase1A", "ContextPaused:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 6, 2);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase2", "ContextPaused:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 2);
		assertThat(EventTracker.events).containsExactly("ContextRestarted:TestCase2", "ContextPaused:TestCase2");
		clearApplicationEvents();

		markContextDirty(TestCase2.class);
		assertThat(EventTracker.events).containsExactly("ContextClosed:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 3);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase2", "ContextPaused:TestCase2");
		clearApplicationEvents();
	}

	@Test
	void topLevelTestClassesWithPauseModeOnContextSwitch() {
		this.contextCache = new DefaultContextCache(DEFAULT_MAX_CONTEXT_CACHE_SIZE, PauseMode.ON_CONTEXT_SWITCH);

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 0, 1);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 1, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 1, 1, 2, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 3, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 3, 2);
		assertThat(EventTracker.events).containsExactly("ContextPaused:TestCase1A", "ContextRefreshed:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 2, 1, 4, 2);
		assertThat(EventTracker.events).containsExactly("ContextPaused:TestCase2", "ContextRestarted:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 2, 1, 5, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 6, 2);
		assertThat(EventTracker.events).containsExactly("ContextPaused:TestCase1A", "ContextRestarted:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		markContextDirty(TestCase2.class);
		assertThat(EventTracker.events).containsExactly("ContextClosed:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 3);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase2");
		clearApplicationEvents();
	}

	@Test
	void topLevelTestClassesWithPauseModeNever() {
		this.contextCache = new DefaultContextCache(DEFAULT_MAX_CONTEXT_CACHE_SIZE, PauseMode.NEVER);

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 0, 1);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase1A");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 1, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 1, 1, 2, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 1, 1, 3, 1);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 3, 2);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1B.class, 2, 1, 4, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase1A.class, 2, 1, 5, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 6, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 2);
		assertThat(EventTracker.events).isEmpty();
		clearApplicationEvents();

		markContextDirty(TestCase2.class);
		assertThat(EventTracker.events).containsExactly("ContextClosed:TestCase2");
		clearApplicationEvents();

		loadCtxAndAssertStats(TestCase2.class, 2, 1, 7, 3);
		assertThat(EventTracker.events).containsExactly("ContextRefreshed:TestCase2");
		clearApplicationEvents();
	}

	private void loadCtxAndAssertStats(Class<?> testClass, int expectedSize, int expectedActiveContextsCount,
			int expectedHitCount, int expectedMissCount) {

		TestContext testContext = TestContextTestUtils.buildTestContext(testClass, contextCache);

		ApplicationContext context = testContext.getApplicationContext();
		assertThat(context).isNotNull();
		assertContextCacheStatistics(contextCache, testClass.getName(), expectedSize, expectedActiveContextsCount,
				expectedHitCount, expectedMissCount);
		testContext.markApplicationContextUnused();
		assertThat(contextCache.getContextUsageCount())
				.as("active contexts in cache (%s)", testClass.getSimpleName()).isZero();
	}

	private void markContextDirty(Class<?> testClass) {
		TestContext testContext = TestContextTestUtils.buildTestContext(testClass, contextCache);
		testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
	}


	@Configuration
	@Import(EventTracker.class)
	static class Config1 {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config2 {
	}

	@ContextConfiguration(classes = Config1.class)
	@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
	private abstract static class AbstractTestCase1 {
	}

	private static class TestCase1A extends AbstractTestCase1 {
	}

	private static class TestCase1B extends AbstractTestCase1 {
	}

	@ContextConfiguration(classes = Config2.class)
	@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
	private static class TestCase2 {
	}

}
