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

import static org.junit.Assert.assertNotNull;
import static org.springframework.test.context.SpringRunnerContextCacheTests.assertContextCacheStatistics;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Unit tests for verifying proper behavior of the {@link ContextCache} in
 * conjunction with cache keys used in {@link TestContext}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SpringRunnerContextCacheTests
 */
public class TestContextCacheKeyTests {

	private ContextCache contextCache = new ContextCache();


	@Before
	public void initialCacheState() {
		assertContextCacheStatistics(contextCache, "initial state", 0, 0, 0);
	}

	private void loadAppCtxAndAssertCacheStats(Class<?> testClass, int expectedSize, int expectedHitCount,
			int expectedMissCount) {
		TestContext testContext = new TestContext(testClass, contextCache);
		ApplicationContext context = testContext.getApplicationContext();
		assertNotNull(context);
		assertContextCacheStatistics(contextCache, testClass.getName(), expectedSize, expectedHitCount,
			expectedMissCount);
	}

	@Test
	public void verifyCacheKeyIsBasedOnContextLoader() {
		loadAppCtxAndAssertCacheStats(AnnotationConfigContextLoaderTestCase.class, 1, 0, 1);
		loadAppCtxAndAssertCacheStats(AnnotationConfigContextLoaderTestCase.class, 1, 1, 1);
		loadAppCtxAndAssertCacheStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 1, 2);
		loadAppCtxAndAssertCacheStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 2, 2);
		loadAppCtxAndAssertCacheStats(AnnotationConfigContextLoaderTestCase.class, 2, 3, 2);
		loadAppCtxAndAssertCacheStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 4, 2);
	}

	@Test
	public void verifyCacheKeyIsBasedOnActiveProfiles() {
		loadAppCtxAndAssertCacheStats(FooBarProfilesTestCase.class, 1, 0, 1);
		loadAppCtxAndAssertCacheStats(FooBarProfilesTestCase.class, 1, 1, 1);
		// Profiles {foo, bar} should hash to the same as {bar,foo}
		loadAppCtxAndAssertCacheStats(BarFooProfilesTestCase.class, 1, 2, 1);
		loadAppCtxAndAssertCacheStats(FooBarProfilesTestCase.class, 1, 3, 1);
		loadAppCtxAndAssertCacheStats(FooBarProfilesTestCase.class, 1, 4, 1);
		loadAppCtxAndAssertCacheStats(BarFooProfilesTestCase.class, 1, 5, 1);
	}


	@Configuration
	static class Config {
	}

	@ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
	private static class AnnotationConfigContextLoaderTestCase {
	}

	@ContextConfiguration(classes = Config.class, loader = CustomAnnotationConfigContextLoader.class)
	private static class CustomAnnotationConfigContextLoaderTestCase {
	}

	private static class CustomAnnotationConfigContextLoader extends AnnotationConfigContextLoader {
	}

	@ActiveProfiles({ "foo", "bar" })
	@ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
	private static class FooBarProfilesTestCase {
	}

	@ActiveProfiles({ "bar", "foo" })
	@ContextConfiguration(classes = Config.class, loader = AnnotationConfigContextLoader.class)
	private static class BarFooProfilesTestCase {
	}

}
