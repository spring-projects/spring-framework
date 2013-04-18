/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.util.ReflectionTestUtils;

import static org.springframework.test.context.SpringRunnerContextCacheTests.*;


import static org.junit.Assert.*;

/**
 * Integration tests for verifying proper behavior of the {@link ContextCache} in
 * conjunction with cache keys used in {@link TestContext}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SpringRunnerContextCacheTests
 */
public class ContextCacheTests {

	private ContextCache contextCache = new ContextCache();


	@Before
	public void initialCacheState() {
		assertContextCacheStatistics(contextCache, "initial state", 0, 0, 0);
		assertParentContextCount(0);
	}

	private void assertParentContextCount(int expected) {
		assertEquals("parent context count", expected, contextCache.getParentContextCount());
	}

	private MergedContextConfiguration getMergedContextConfiguration(TestContext testContext) {
		return (MergedContextConfiguration) ReflectionTestUtils.getField(testContext, "mergedContextConfiguration");
	}

	private ApplicationContext loadContext(Class<?> testClass) {
		TestContext testContext = new TestContext(testClass, contextCache);
		return testContext.getApplicationContext();
	}

	private void loadCtxAndAssertStats(Class<?> testClass, int expectedSize, int expectedHitCount, int expectedMissCount) {
		assertNotNull(loadContext(testClass));
		assertContextCacheStatistics(contextCache, testClass.getName(), expectedSize, expectedHitCount,
			expectedMissCount);
	}

	@Test
	public void verifyCacheKeyIsBasedOnContextLoader() {
		loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 1, 0, 1);
		loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 1, 1, 1);
		loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 1, 2);
		loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 2, 2);
		loadCtxAndAssertStats(AnnotationConfigContextLoaderTestCase.class, 2, 3, 2);
		loadCtxAndAssertStats(CustomAnnotationConfigContextLoaderTestCase.class, 2, 4, 2);
	}

	@Test
	public void verifyCacheKeyIsBasedOnActiveProfiles() {
		loadCtxAndAssertStats(FooBarProfilesTestCase.class, 1, 0, 1);
		loadCtxAndAssertStats(FooBarProfilesTestCase.class, 1, 1, 1);
		// Profiles {foo, bar} should hash to the same as {bar,foo}
		loadCtxAndAssertStats(BarFooProfilesTestCase.class, 1, 2, 1);
		loadCtxAndAssertStats(FooBarProfilesTestCase.class, 1, 3, 1);
		loadCtxAndAssertStats(FooBarProfilesTestCase.class, 1, 4, 1);
		loadCtxAndAssertStats(BarFooProfilesTestCase.class, 1, 5, 1);
	}

	@Test
	public void verifyCacheBehaviorForContextHierarchies() {
		int size = 0;
		int hits = 0;
		int misses = 0;

		// Level 1
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel1TestCase.class, ++size, hits, ++misses);
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel1TestCase.class, size, ++hits, misses);

		// Level 2
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, ++size /* L2 */, ++hits /* L1 */,
			++misses /* L2 */);
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, size, ++hits /* L2 */, misses);
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel2TestCase.class, size, ++hits /* L2 */, misses);

		// Level 3-A
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3aTestCase.class, ++size /* L3A */, ++hits /* L2 */,
			++misses /* L3A */);
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3aTestCase.class, size, ++hits /* L3A */, misses);

		// Level 3-B
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3bTestCase.class, ++size /* L3B */, ++hits /* L2 */,
			++misses /* L3B */);
		loadCtxAndAssertStats(ClassHierarchyContextHierarchyLevel3bTestCase.class, size, ++hits /* L3B */, misses);
	}

	@Test
	public void removeContextHierarchyCacheLevel1() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 1
		// Should also remove Levels 2, 3-A, and 3-B, leaving nothing.
		contextCache.remove(getMergedContextConfiguration(testContext3a).getParent().getParent(),
			HierarchyMode.CURRENT_LEVEL);
		assertContextCacheStatistics(contextCache, "removed level 1", 0, 1, 4);
		assertParentContextCount(0);
	}

	@Test
	public void removeContextHierarchyCacheLevel1WithExhaustiveMode() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 1
		// Should also remove Levels 2, 3-A, and 3-B, leaving nothing.
		contextCache.remove(getMergedContextConfiguration(testContext3a).getParent().getParent(),
			HierarchyMode.EXHAUSTIVE);
		assertContextCacheStatistics(contextCache, "removed level 1", 0, 1, 4);
		assertParentContextCount(0);
	}

	@Test
	public void removeContextHierarchyCacheLevel2() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 2
		// Should also remove Levels 3-A and 3-B, leaving only Level 1 as a context in the
		// cache but also removing the Level 1 hierarchy since all children have been
		// removed.
		contextCache.remove(getMergedContextConfiguration(testContext3a).getParent(), HierarchyMode.CURRENT_LEVEL);
		assertContextCacheStatistics(contextCache, "removed level 2", 1, 1, 4);
		assertParentContextCount(0);
	}

	@Test
	public void removeContextHierarchyCacheLevel2WithExhaustiveMode() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 2
		// Should wipe the cache
		contextCache.remove(getMergedContextConfiguration(testContext3a).getParent(), HierarchyMode.EXHAUSTIVE);
		assertContextCacheStatistics(contextCache, "removed level 2", 0, 1, 4);
		assertParentContextCount(0);
	}

	@Test
	public void removeContextHierarchyCacheLevel3Then2() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 3-A
		contextCache.remove(getMergedContextConfiguration(testContext3a), HierarchyMode.CURRENT_LEVEL);
		assertContextCacheStatistics(contextCache, "removed level 3-A", 3, 1, 4);
		assertParentContextCount(2);

		// Remove Level 2
		// Should also remove Level 3-B, leaving only Level 1.
		contextCache.remove(getMergedContextConfiguration(testContext3b).getParent(), HierarchyMode.CURRENT_LEVEL);
		assertContextCacheStatistics(contextCache, "removed level 2", 1, 1, 4);
		assertParentContextCount(0);
	}

	@Test
	public void removeContextHierarchyCacheLevel3Then2WithExhaustiveMode() {

		// Load Level 3-A
		TestContext testContext3a = new TestContext(ClassHierarchyContextHierarchyLevel3aTestCase.class, contextCache);
		testContext3a.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A", 3, 0, 3);
		assertParentContextCount(2);

		// Load Level 3-B
		TestContext testContext3b = new TestContext(ClassHierarchyContextHierarchyLevel3bTestCase.class, contextCache);
		testContext3b.getApplicationContext();
		assertContextCacheStatistics(contextCache, "level 3, A and B", 4, 1, 4);
		assertParentContextCount(2);

		// Remove Level 3-A
		// Should wipe the cache.
		contextCache.remove(getMergedContextConfiguration(testContext3a), HierarchyMode.EXHAUSTIVE);
		assertContextCacheStatistics(contextCache, "removed level 3-A", 0, 1, 4);
		assertParentContextCount(0);

		// Remove Level 2
		// Should not actually do anything since the cache was cleared in the
		// previous step. So the stats should remain the same.
		contextCache.remove(getMergedContextConfiguration(testContext3b).getParent(), HierarchyMode.EXHAUSTIVE);
		assertContextCacheStatistics(contextCache, "removed level 2", 0, 1, 4);
		assertParentContextCount(0);
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

	@ContextHierarchy({ @ContextConfiguration })
	private static class ClassHierarchyContextHierarchyLevel1TestCase {

		@Configuration
		static class Level1Config {

		}
	}

	@ContextHierarchy({ @ContextConfiguration })
	private static class ClassHierarchyContextHierarchyLevel2TestCase extends
			ClassHierarchyContextHierarchyLevel1TestCase {

		@Configuration
		static class Level2Config {

		}
	}

	@ContextHierarchy({ @ContextConfiguration })
	private static class ClassHierarchyContextHierarchyLevel3aTestCase extends
			ClassHierarchyContextHierarchyLevel2TestCase {

		@Configuration
		static class Level3aConfig {

		}
	}

	@ContextHierarchy({ @ContextConfiguration })
	private static class ClassHierarchyContextHierarchyLevel3bTestCase extends
			ClassHierarchyContextHierarchyLevel2TestCase {

		@Configuration
		static class Level3bConfig {

		}
	}

}
