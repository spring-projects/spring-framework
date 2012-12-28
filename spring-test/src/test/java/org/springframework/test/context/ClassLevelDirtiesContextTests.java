/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.TrackingRunListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * JUnit 4 based integration test which verifies correct {@link ContextCache
 * application context caching} in conjunction with the
 * {@link SpringJUnit4ClassRunner} and the {@link DirtiesContext
 * &#064;DirtiesContext} annotation at the class level.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
public class ClassLevelDirtiesContextTests {

	private static final AtomicInteger cacheHits = new AtomicInteger(0);
	private static final AtomicInteger cacheMisses = new AtomicInteger(0);


	/**
	 * Asserts the statistics of the supplied context cache.
	 *
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	private static final void assertCacheStats(String usageScenario, int expectedSize, int expectedHitCount,
			int expectedMissCount) {

		ContextCache contextCache = TestContextManager.contextCache;
		assertEquals("Verifying number of contexts in cache (" + usageScenario + ").", expectedSize,
			contextCache.size());
		assertEquals("Verifying number of cache hits (" + usageScenario + ").", expectedHitCount,
			contextCache.getHitCount());
		assertEquals("Verifying number of cache misses (" + usageScenario + ").", expectedMissCount,
			contextCache.getMissCount());
	}

	private static final void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) {
		final int expectedTestFailureCount = 0;
		final int expectedTestStartedCount = expectedTestCount;
		final int expectedTestFinishedCount = expectedTestCount;

		TrackingRunListener listener = new TrackingRunListener();
		JUnitCore jUnitCore = new JUnitCore();
		jUnitCore.addListener(listener);
		jUnitCore.run(testClass);

		assertEquals("Verifying number of failures for test class [" + testClass + "].", expectedTestFailureCount,
			listener.getTestFailureCount());
		assertEquals("Verifying number of tests started for test class [" + testClass + "].", expectedTestStartedCount,
			listener.getTestStartedCount());
		assertEquals("Verifying number of tests finished for test class [" + testClass + "].",
			expectedTestFinishedCount, listener.getTestFinishedCount());
	}

	@BeforeClass
	public static void verifyInitialCacheState() {
		ContextCache contextCache = TestContextManager.contextCache;
		contextCache.clear();
		contextCache.clearStatistics();
		cacheHits.set(0);
		cacheMisses.set(0);
		assertCacheStats("BeforeClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	public void verifyDirtiesContextBehavior() throws Exception {

		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with clean test method and default class mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertCacheStats("after inherited class-level @DirtiesContext with clean test method and default class mode",
			0, cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertCacheStats("after inherited class-level @DirtiesContext with clean test method and AFTER_CLASS mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertCacheStats("after class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase.class, 3);
		assertCacheStats(
			"after inherited class-level @DirtiesContext with clean test method and AFTER_EACH_TEST_METHOD mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.addAndGet(2));
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with dirty test method", 0, cacheHits.incrementAndGet(),
			cacheMisses.get());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after inherited class-level @DirtiesContext with dirty test method", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after inherited class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		runTestClassAndAssertStats(InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase.class, 1);
		assertCacheStats("after inherited class-level @DirtiesContext with dirty test method", 0, cacheHits.get(),
			cacheMisses.incrementAndGet());
		assertBehaviorForCleanTestCase();

		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase.class, 1);
		assertCacheStats("after class-level @DirtiesContext with clean test method and AFTER_CLASS mode", 0,
			cacheHits.incrementAndGet(), cacheMisses.get());
	}

	private void assertBehaviorForCleanTestCase() {
		runTestClassAndAssertStats(CleanTestCase.class, 1);
		assertCacheStats("after clean test class", 1, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	@AfterClass
	public static void verifyFinalCacheState() {
		assertCacheStats("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}


	// -------------------------------------------------------------------

	@RunWith(SpringJUnit4ClassRunner.class)
	@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class })
	@ContextConfiguration("/org/springframework/test/context/junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml")
	public static abstract class BaseTestCase {

		@Autowired
		protected ApplicationContext applicationContext;


		protected void assertApplicationContextWasAutowired() {
			assertNotNull("The application context should have been autowired.", this.applicationContext);
		}
	}

	public static final class CleanTestCase extends BaseTestCase {

		@Test
		public void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}

	}

	@DirtiesContext
	public static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends BaseTestCase {

		@Test
		public void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	public static class InheritedClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
	public static class ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends BaseTestCase {

		@Test
		public void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	public static class InheritedClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase extends
			ClassLevelDirtiesContextWithCleanMethodsAndAfterClassModeTestCase {
	}

	@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
	public static class ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends BaseTestCase {

		@Test
		public void verifyContextWasAutowired1() {
			assertApplicationContextWasAutowired();
		}

		@Test
		public void verifyContextWasAutowired2() {
			assertApplicationContextWasAutowired();
		}

		@Test
		public void verifyContextWasAutowired3() {
			assertApplicationContextWasAutowired();
		}
	}

	public static class InheritedClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase extends
			ClassLevelDirtiesContextWithAfterEachTestMethodModeTestCase {
	}

	@DirtiesContext
	public static class ClassLevelDirtiesContextWithDirtyMethodsTestCase extends BaseTestCase {

		@Test
		@DirtiesContext
		public void dirtyContext() {
			assertApplicationContextWasAutowired();
		}
	}

	public static class InheritedClassLevelDirtiesContextWithDirtyMethodsTestCase extends
			ClassLevelDirtiesContextWithDirtyMethodsTestCase {
	}

}
