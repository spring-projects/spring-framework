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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.junit.Assert.*;
import static org.springframework.test.context.cache.ContextCacheTestUtils.*;

/**
 * JUnit 4 based unit test which verifies correct {@link ContextCache
 * application context caching} in conjunction with the
 * {@link SpringJUnit4ClassRunner} and the {@link DirtiesContext
 * &#064;DirtiesContext} annotation at the method level.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextCacheTests
 * @see LruContextCacheTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
@ContextConfiguration("../junit4/SpringJUnit4ClassRunnerAppCtxTests-context.xml")
public class SpringRunnerContextCacheTests {

	private static ApplicationContext dirtiedApplicationContext;

	@Autowired
	protected ApplicationContext applicationContext;

	@BeforeClass
	public static void verifyInitialCacheState() {
		dirtiedApplicationContext = null;
		resetContextCache();
		assertContextCacheStatistics("BeforeClass", 0, 0, 0);
	}

	@AfterClass
	public static void verifyFinalCacheState() {
		assertContextCacheStatistics("AfterClass", 1, 1, 2);
	}

	@Test
	@DirtiesContext
	public void dirtyContext() {
		assertContextCacheStatistics("dirtyContext()", 1, 0, 1);
		assertNotNull("The application context should have been autowired.", this.applicationContext);
		SpringRunnerContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	public void verifyContextDirty() {
		assertContextCacheStatistics("verifyContextWasDirtied()", 1, 0, 2);
		assertNotNull("The application context should have been autowired.", this.applicationContext);
		assertNotSame("The application context should have been 'dirtied'.",
			SpringRunnerContextCacheTests.dirtiedApplicationContext, this.applicationContext);
		SpringRunnerContextCacheTests.dirtiedApplicationContext = this.applicationContext;
	}

	@Test
	public void verifyContextNotDirty() {
		assertContextCacheStatistics("verifyContextWasNotDirtied()", 1, 1, 2);
		assertNotNull("The application context should have been autowired.", this.applicationContext);
		assertSame("The application context should NOT have been 'dirtied'.",
			SpringRunnerContextCacheTests.dirtiedApplicationContext, this.applicationContext);
	}

}
