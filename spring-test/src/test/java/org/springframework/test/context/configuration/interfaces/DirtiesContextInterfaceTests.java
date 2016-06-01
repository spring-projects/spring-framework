/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.configuration.interfaces;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;
import static org.springframework.test.context.cache.ContextCacheTestUtils.*;
import static org.springframework.test.context.junit4.JUnitTestingUtils.*;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@RunWith(JUnit4.class)
public class DirtiesContextInterfaceTests {

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

	@AfterClass
	public static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 0, cacheHits.get(), cacheMisses.get());
	}

	@Test
	public void verifyDirtiesContextBehavior() throws Exception {
		runTestClassAndAssertStats(ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase.class, 1);
		assertContextCacheStatistics("after class-level @DirtiesContext with clean test method and default class mode",
			0, cacheHits.get(), cacheMisses.incrementAndGet());
	}

	private void runTestClassAndAssertStats(Class<?> testClass, int expectedTestCount) throws Exception {
		runTestsAndAssertCounters(testClass, expectedTestCount, 0, expectedTestCount, 0, 0);
	}


	@RunWith(SpringRunner.class)
	public static class ClassLevelDirtiesContextWithCleanMethodsAndDefaultModeTestCase
			implements DirtiesContextTestInterface {

		@Autowired
		ApplicationContext applicationContext;


		@Test
		public void verifyContextWasAutowired() {
			assertNotNull("The application context should have been autowired.", this.applicationContext);
		}


		@Configuration
		static class Config {
			/* no beans */
		}

	}

}
