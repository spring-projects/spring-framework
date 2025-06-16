/*
 * Copyright 2002-2025 the original author or authors.
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testng.TestNG;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.context.testng.TrackingTestNGTestListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.cache.ContextCacheTestUtils.assertContextCacheStatistics;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * JUnit based integration test which verifies correct {@linkplain ContextCache
 * application context caching} in conjunction with Spring's TestNG support
 * and {@link DirtiesContext @DirtiesContext} at the class level.
 *
 * <p>This class is a direct copy of {@link ClassLevelDirtiesContextTests},
 * modified to verify behavior in conjunction with TestNG.
 *
 * @author Sam Brannen
 * @since 4.2
 */
class ClassLevelDirtiesContextTestNGTests {

	private static final AtomicInteger cacheHits = new AtomicInteger();
	private static final AtomicInteger cacheMisses = new AtomicInteger();


	@BeforeAll
	static void verifyInitialCacheState() {
		resetContextCache();
		// Reset static counters in case tests are run multiple times in a test suite --
		// for example, via JUnit's @Suite.
		cacheHits.set(0);
		cacheMisses.set(0);
		assertContextCacheStatistics("BeforeClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	void verifyDirtiesContextBehavior() {

		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and default class mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and default class mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and AFTER_CLASS mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertContextCacheStatistics(
			"after class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertContextCacheStatistics(
			"after inherited class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.get(), cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertContextCacheStatistics("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.get(), cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
	}

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) {
		final int expectedTestFailureCount = 0;
		final int expectedTestStartedCount = expectedTestCount;
		final int expectedTestFinishedCount = expectedTestCount;

		final TrackingTestNGTestListener listener = new TrackingTestNGTestListener();
		final TestNG testNG = new TestNG();
		testNG.addListener(listener);
		testNG.setTestClasses(new Class<?>[] { testClass });
		testNG.setVerbose(0);
		testNG.run();

		assertThat(listener.testFailureCount).as("Failures for test class [" + testClass + "].").isEqualTo(expectedTestFailureCount);
		assertThat(listener.testStartCount).as("Tests started for test class [" + testClass + "].").isEqualTo(expectedTestStartedCount);
		assertThat(listener.testSuccessCount).as("Successful tests for test class [" + testClass + "].").isEqualTo(expectedTestFinishedCount);
	}

	private void assertBehaviorForCleanTestCase() {
		runTestClassAndAssertStats(CleanTestCase.class, 1);
		assertContextCacheStatistics("after clean test class", 1, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	@AfterAll
	static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}


	// -------------------------------------------------------------------

	@ContextConfiguration
	// Ensure that we do not include the EventPublishingTestExecutionListener
	// since it will access the ApplicationContext for each method in the
	// TestExecutionListener API, thus distorting our cache hit/miss results.
	@TestExecutionListeners({
		DirtiesContextBeforeModesTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class
	})
	abstract static class BaseTestCase extends AbstractTestNGSpringContextTests {

		@Configuration(proxyBeanMethods = false)
		static class Config {
			/* no beans */
		}


		@Autowired
		protected ApplicationContext applicationContext;


		protected void assertApplicationContextWasAutowired() {
			assertThat(this.applicationContext).as("The application context should have been autowired.").isNotNull();
		}
	}

	static final class CleanTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}

	}

	@DirtiesContext
	static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
	static class ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
	static class ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		void verifyContextWasAutowired1() {
			assertApplicationContextWasAutowired();
		}

		@org.testng.annotations.Test
		void verifyContextWasAutowired2() {
			assertApplicationContextWasAutowired();
		}

		@org.testng.annotations.Test
		void verifyContextWasAutowired3() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends
			ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase {
	}

	@DirtiesContext
	static class ClassLevelDirtiesContextWithDirtyMethodsTestCase extends BaseTestCase {

		@org.testng.annotations.Test
		@DirtiesContext
		void dirtyContext() {
			assertApplicationContextWasAutowired();
		}
	}

	static class InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase extends
			ClassLevelDirtiesContextWithDirtyMethodsTestCase {
	}

}
