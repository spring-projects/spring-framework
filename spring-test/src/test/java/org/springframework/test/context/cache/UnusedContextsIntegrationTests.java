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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.cache.ContextCache.PauseMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.context.cache.ContextCacheTestUtils.resetContextCache;

/**
 * Integration tests for pausing and restarting "unused" contexts.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see TestContext#markApplicationContextUnused()
 */
class UnusedContextsIntegrationTests {

	@BeforeEach
	@AfterEach
	void clearApplicationEventsAndResetContextCache() {
		resetContextCache();
		EventTracker.events.clear();
	}

	@Test
	void topLevelTestClassesWithDifferentApplicationContexts() {
		runTestClasses(6,
				TestCaseConfig1A.class,
				TestCaseConfig1B.class,
				TestCaseConfig2.class,
				TestCaseConfig3.class,
				TestCaseConfig4.class,
				TestCaseConfig5.class);

		assertThat(EventTracker.events).containsExactly(

			// --- TestCaseConfig1A --------------------------------------------
			"ContextRefreshed:TestCaseConfig1A",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCaseConfig1A",

			// --- TestCaseConfig1B --------------------------------------------
			// Here we expect a BeforeTestClass event, since TestCaseConfig1B
			// uses the same context as TestCaseConfig1A.
			"BeforeTestClass:TestCaseConfig1B",
			"AfterTestClass:TestCaseConfig1B",

			// --- TestCaseConfig2 ---------------------------------------------
			"ContextPaused:TestCaseConfig1A",
			"ContextRefreshed:TestCaseConfig2",
			"AfterTestClass:TestCaseConfig2",

			// --- TestCaseConfig3 ---------------------------------------------
			"ContextPaused:TestCaseConfig2",
			"ContextRefreshed:TestCaseConfig3",
			"AfterTestClass:TestCaseConfig3",
			// Closed instead of Paused, since TestCaseConfig3 uses @DirtiesContext
			"ContextClosed:TestCaseConfig3",

			// --- TestCaseConfig4 ---------------------------------------------
			"ContextRefreshed:TestCaseConfig4",
			"AfterTestClass:TestCaseConfig4",

			// --- TestCaseConfig5 ---------------------------------------------
			"ContextPaused:TestCaseConfig4",
			"ContextRefreshed:TestCaseConfig5",
			"AfterTestClass:TestCaseConfig5"
		);
	}

	/**
	 * Since {@link PauseMode#ON_CONTEXT_SWITCH} is now the default, there are
	 * no {@code ContextPausedEvent} or {@code ContextRestartedEvent} events
	 * when all test classes share the same context.
	 */
	@Test
	void topLevelTestClassesWithSharedApplicationContext() {
		runTestClasses(5, TestCase1.class, TestCase2.class, TestCase3.class, TestCase4.class, TestCase5.class);

		assertThat(EventTracker.events).containsExactly(

			// --- TestCase1 -----------------------------------------------
			// Refreshed, since this is the first time the context is loaded.
			"ContextRefreshed:TestCase1",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase1",

			// --- TestCase2 -----------------------------------------------
			"BeforeTestClass:TestCase2",
			"AfterTestClass:TestCase2",

			// --- TestCase3 -----------------------------------------------
			"BeforeTestClass:TestCase3",
			"AfterTestClass:TestCase3",
			// Closed instead of Paused, since TestCase3 uses @DirtiesContext
			"ContextClosed:TestCase1",

			// --- TestCase4 -----------------------------------------------
			// Refreshed, since TestCase3 uses @DirtiesContext
			"ContextRefreshed:TestCase4",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase4",

			// --- TestCase5 -----------------------------------------------
			"BeforeTestClass:TestCase5",
			"AfterTestClass:TestCase5"
		);
	}

	@Test
	void testClassesInNestedTestHierarchy() {
		testClassesInNestedTestHierarchy(EnclosingTestCase.class, false);
	}

	@Test
	void testClassesInNestedTestHierarchyWithTestInstanceLifecyclePerClass() {
		testClassesInNestedTestHierarchy(TestInstancePerClassEnclosingTestCase.class, true);
	}

	private void testClassesInNestedTestHierarchy(Class<?> enclosingClass, boolean expectBeforeTestClassEvent) {
		// We also run a stand-alone top-level test class after the nested hierarchy,
		// in order to verify what happens for a context switch from a nested hierarchy
		// to something else.
		runTestClasses(7, enclosingClass, TestCase1.class);

		String enclosingClassName = enclosingClass.getSimpleName();

		String[] events = {
			// --- EnclosingTestCase -------------------------------------------
			"ContextRefreshed:" + enclosingClassName,
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.

				// --- NestedTestCase1 -----------------------------------------
				// No Refreshed or Restarted event, since NestedTestCase1 shares the
				// active context used by EnclosingTestCase.
				"BeforeTestClass:NestedTestCase1",

					// --- OverridingNestedTestCase1 ---------------------------
					"ContextRefreshed:OverridingNestedTestCase1",
					// No BeforeTestClass, since EventPublishingTestExecutionListener
					// only publishes events for a context that has already been loaded.

						// --- InheritingNestedTestCase ------------------------
						// No Refreshed or Restarted event, since InheritingNestedTestCase
						// shares the active context used by OverridingNestedTestCase1.
						"BeforeTestClass:InheritingNestedTestCase",
						"AfterTestClass:InheritingNestedTestCase",
						// No Paused event, since OverridingNestedTestCase1 is still
						// using the context

					"AfterTestClass:OverridingNestedTestCase1",
					// No Paused event, since OverridingNestedTestCase2 will reuse
					// the context

					// --- OverridingNestedTestCase2 ---------------------------
					// No Restarted event, since OverridingNestedTestCase2 will reuse
					// the context
					"BeforeTestClass:OverridingNestedTestCase2",
					"AfterTestClass:OverridingNestedTestCase2",
					"ContextPaused:OverridingNestedTestCase1",

				"AfterTestClass:NestedTestCase1",
				// No Paused event, since EnclosingTestCase is still using the context

				// --- NestedTestCase2 -----------------------------------------
				// Refreshed, since this is the first time the context is loaded.
				"ContextRefreshed:NestedTestCase2",
				"AfterTestClass:NestedTestCase2",

			// Paused, since the context used by NestedTestCase2 is no longer used,
			// and EventPublishingTestExecutionListener.afterTestClass() "gets" the
			// context for the enclosing class again, which constitutes a context switch.
			"ContextPaused:NestedTestCase2",
			"AfterTestClass:" + enclosingClassName,

			// --- TestCase1 ---------------------------------------------------
			// Paused, since the context for the enclosing class is no longer used.
			"ContextPaused:" + enclosingClassName,
			// Refreshed, since this is the first time the context is loaded.
			"ContextRefreshed:TestCase1",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase1",
		};

		List<String> eventsList = new ArrayList<>();
		Collections.addAll(eventsList, events);
		if (expectBeforeTestClassEvent) {
			eventsList.add(1, "BeforeTestClass:" + enclosingClassName);
		}
		assertThat(EventTracker.events).containsExactlyElementsOf(eventsList);
	}

	@Test
	void testClassesWithContextHierarchies() {
		// We also run a stand-alone top-level test class after the context hierarchy,
		// in order to verify what happens for a context switch from a context hierarchy
		// to something else.
		runTestClasses(6,
			ContextHierarchyLevel1TestCase.class,
			ContextHierarchyLevel2TestCase.class,
			ContextHierarchyLevel3a1TestCase.class,
			ContextHierarchyLevel3a2TestCase.class,
			ContextHierarchyLevel3bTestCase.class,
			TestCase1.class
		);

		assertThat(EventTracker.events).containsExactly(

			// --- ContextHierarchyLevel1TestCase ------------------------------
			"ContextRefreshed:ContextHierarchyLevel1TestCase",
			"AfterTestClass:ContextHierarchyLevel1TestCase",
			// No Paused event, since ContextHierarchyLevel2TestCase uses
			// ContextHierarchyLevel1TestCase as its parent.

			// --- ContextHierarchyLevel2TestCase ------------------------------
			"ContextRefreshed:ContextHierarchyLevel2TestCase",
			"AfterTestClass:ContextHierarchyLevel2TestCase",
			// No Paused events, since ContextHierarchyLevel3a1TestCase uses
			// ContextHierarchyLevel2TestCase and ContextHierarchyLevel1TestCase
			// as its parents.

			// --- ContextHierarchyLevel3a1TestCase -----------------------------
			"ContextRefreshed:ContextHierarchyLevel3a1TestCase",
			"AfterTestClass:ContextHierarchyLevel3a1TestCase",
			// No Paused events, since ContextHierarchyLevel3a2TestCase also uses
			// ContextHierarchyLevel2TestCase and ContextHierarchyLevel1TestCase
			// as its parents.

			// --- ContextHierarchyLevel3a2TestCase -----------------------------
			"BeforeTestClass:ContextHierarchyLevel3a2TestCase",
			"AfterTestClass:ContextHierarchyLevel3a2TestCase",

			// --- ContextHierarchyLevel3bTestCase -----------------------------
			// We see a ContextPausedEvent here, since ContextHierarchyLevel3a1TestCase
			// and ContextHierarchyLevel3a2TestCase are no longer active and we
			// are "switching" to ContextHierarchyLevel3bTestCase as the active context.
			// In other words, we pause the inactive context before refreshing the
			// new, active context.
			"ContextPaused:ContextHierarchyLevel3a1TestCase",
			"ContextRefreshed:ContextHierarchyLevel3bTestCase",
			"AfterTestClass:ContextHierarchyLevel3bTestCase",

			// --- TestCase1 ---------------------------------------------------
			// Paused, since the previous context hierarchy is no longer used.
			// Note that the pause order is bottom up.
			"ContextPaused:ContextHierarchyLevel3bTestCase",
			"ContextPaused:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase",
			// Refreshed, since this is the first time the context is loaded.
			"ContextRefreshed:TestCase1",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase1"
		);
	}


	private static void runTestClasses(int expectedTestCount, Class<?>... classes) {
		EngineTestKit.engine("junit-jupiter")
			.selectors(selectClasses(classes))
			.execute()
			.testEvents()
			.assertStatistics(stats -> stats.started(expectedTestCount).succeeded(expectedTestCount));
	}


	@SpringJUnitConfig(EventTracker.class)
	@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
	private abstract static class AbstractTestCase {

		@Test
		void test() {
			// no-op
		}
	}

	static class TestCase1 extends AbstractTestCase {
	}

	static class TestCase2 extends AbstractTestCase {
	}

	@DirtiesContext
	static class TestCase3 extends AbstractTestCase {
	}

	static class TestCase4 extends AbstractTestCase {
	}

	static class TestCase5 extends AbstractTestCase {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config1 {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config2 {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config3 {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config4 {
	}

	@Configuration
	@Import(EventTracker.class)
	static class Config5 {
	}

	@SpringJUnitConfig(Config1.class)
	static class TestCaseConfig1A extends AbstractTestCase {
	}

	@SpringJUnitConfig(Config1.class)
	static class TestCaseConfig1B extends AbstractTestCase {
	}

	@SpringJUnitConfig(Config2.class)
	static class TestCaseConfig2 extends AbstractTestCase {
	}

	@SpringJUnitConfig(Config3.class)
	@DirtiesContext
	static class TestCaseConfig3 extends AbstractTestCase {
	}

	@SpringJUnitConfig(Config4.class)
	static class TestCaseConfig4 extends AbstractTestCase {
	}

	@SpringJUnitConfig(Config5.class)
	static class TestCaseConfig5 extends AbstractTestCase {
	}

	@SpringJUnitConfig(EventTracker.class)
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
	@TestPropertySource(properties = "magicKey = puzzle")
	static class EnclosingTestCase {

		@Test
		void test(@Value("${magicKey}") String magicKey) {
			assertThat(magicKey).isEqualTo("puzzle");
		}

		@Nested
		@Order(1)
		class NestedTestCase1 {

			@Test
			void test(@Value("${magicKey}") String magicKey) {
				assertThat(magicKey).isEqualTo("puzzle");
			}

			/**
			 * Duplicates configuration of {@link OverridingNestedTestCase2}.
			 */
			@Nested
			@Order(1)
			@NestedTestConfiguration(OVERRIDE)
			@SpringJUnitConfig(EventTracker.class)
			@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
			@TestPropertySource(properties = "magicKey = enigma")
			class OverridingNestedTestCase1 {

				@Test
				void test(@Value("${magicKey}") String magicKey) {
					assertThat(magicKey).isEqualTo("enigma");
				}

				@Nested
				@NestedTestConfiguration(INHERIT)
				class InheritingNestedTestCase {

					@Test
					void test(@Value("${magicKey}") String magicKey) {
						assertThat(magicKey).isEqualTo("enigma");
					}
				}
			}

			/**
			 * Duplicates configuration of {@link OverridingNestedTestCase1}.
			 */
			@Nested
			@Order(2)
			@NestedTestConfiguration(OVERRIDE)
			@SpringJUnitConfig(EventTracker.class)
			@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
			@TestPropertySource(properties = "magicKey = enigma")
			class OverridingNestedTestCase2 {

				@Test
				void test(@Value("${magicKey}") String magicKey) {
					assertThat(magicKey).isEqualTo("enigma");
				}
			}
		}

		@Nested
		@Order(2)
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(EventTracker.class)
		@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
		@TestPropertySource(properties = "magicKey = enigma2")
		class NestedTestCase2 {

			@Test
			void test(@Value("${magicKey}") String magicKey) {
				assertThat(magicKey).isEqualTo("enigma2");
			}
		}
	}

	@TestInstance(Lifecycle.PER_CLASS)
	static class TestInstancePerClassEnclosingTestCase extends EnclosingTestCase {
	}

}
