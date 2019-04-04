/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
import static org.springframework.test.context.cache.ContextCacheTestUtils.*;
import static org.springframework.test.context.junit4.JUnitTestingUtils.*;

/**
 * JUnit 4 based integration test which verifies correct {@linkplain ContextCache
 * application context caching} in conjunction with the {@link SpringRunner} and
 * {@link DirtiesContext @DirtiesContext} at the class level.
 *
 * @author Sam Brannen
 * @since 3.0
 */
@RunWith(JUnit4.class)
public class ClassLevelDirtiesContextTests {

	private static final AtomicInteger cacheHits = new AtomicInteger(0);
	private static final AtomicInteger cacheMisses = new AtomicInteger(0);


	@BeforeClass
	public static void verifyInitialCacheState() {
		resetContextCache();
		// Reset static counters in case tests are run multiple times in a test suite --
		// for example, via JUnit's @Suite.
		cacheHits.set(0);
		cacheMisses.set(0);
		assertContextCacheStatistics("BeforeClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	public void verifyDirtiesContextBehavior() throws Exception {

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

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) throws Exception {
		runTestsAndAssertCounters(testClass, expectedTestCount, 0, expectedTestCount, 0, 0);
	}

	private void assertBehaviorForCleanTestCase() throws Exception {
		runTestClassAndAssertStats(CleanTestCase.class, 1);
		assertContextCacheStatistics("after clean test class", 1, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	@AfterClass
	public static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}


	// -------------------------------------------------------------------

	@RunWith(SpringRunner.class)
	@ContextConfiguration
	static abstract class BaseTestCase {

		@Configuration
		static class Config {
			/* no beans */
		}


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
