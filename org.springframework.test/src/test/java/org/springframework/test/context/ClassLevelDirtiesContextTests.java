/*
 * Copyright 2002-2009 the original author or authors.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
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

	/**
	 * Asserts the statistics of the supplied context cache.
	 * 
	 * @param usageScenario the scenario in which the statistics are used
	 * @param expectedSize the expected number of contexts in the cache
	 * @param expectedHitCount the expected hit count
	 * @param expectedMissCount the expected miss count
	 */
	private static final void assertContextCacheStatistics(String usageScenario, int expectedSize,
			int expectedHitCount, int expectedMissCount) {

		ContextCache contextCache = TestContextManager.contextCache;
		assertEquals("Verifying number of contexts in cache (" + usageScenario + ").", expectedSize,
			contextCache.size());
		assertEquals("Verifying number of cache hits (" + usageScenario + ").", expectedHitCount,
			contextCache.getHitCount());
		assertEquals("Verifying number of cache misses (" + usageScenario + ").", expectedMissCount,
			contextCache.getMissCount());
	}

	private static final void runTestClassAndAssertRunListenerStats(Class<?> testClass) {
		final int expectedTestFailureCount = 0;
		final int expectedTestStartedCount = 1;
		final int expectedTestFinishedCount = 1;

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
		assertContextCacheStatistics("BeforeClass", 0, 0, 0);
	}

	@AfterClass
	public static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, 3, 5);
	}

	@Test
	public void verifyDirtiesContextBehavior() throws Exception {

		int hits = 0;
		int misses = 0;

		runTestClassAndAssertRunListenerStats(CleanTestCase.class);
		assertContextCacheStatistics("after clean test class", 1, hits, ++misses);

		runTestClassAndAssertRunListenerStats(ClassLevelDirtiesContextWithCleanMethodsTestCase.class);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method", 0, ++hits, misses);

		runTestClassAndAssertRunListenerStats(CleanTestCase.class);
		assertContextCacheStatistics("after clean test class", 1, hits, ++misses);

		runTestClassAndAssertRunListenerStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, ++hits, misses);

		runTestClassAndAssertRunListenerStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, hits, ++misses);

		runTestClassAndAssertRunListenerStats(ClassLevelDirtiesContextWithDirtyMethodsTestCase.class);
		assertContextCacheStatistics("after class-level @DirtiesContext with dirty test method", 0, hits, ++misses);

		runTestClassAndAssertRunListenerStats(CleanTestCase.class);
		assertContextCacheStatistics("after clean test class", 1, hits, ++misses);

		runTestClassAndAssertRunListenerStats(ClassLevelDirtiesContextWithCleanMethodsTestCase.class);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method", 0, ++hits, misses);
	}


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
	public static final class ClassLevelDirtiesContextWithCleanMethodsTestCase extends BaseTestCase {

		@Test
		public void verifyContextWasAutowired() {
			assertApplicationContextWasAutowired();
		}
	}

	@DirtiesContext
	public static final class ClassLevelDirtiesContextWithDirtyMethodsTestCase extends BaseTestCase {

		@Test
		@DirtiesContext
		public void dirtyContext() {
			assertApplicationContextWasAutowired();
		}
	}

}
