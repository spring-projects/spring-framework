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
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextCustomizerFactories;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasses;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

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
	void clearApplicationEvents() {
		EventTracker.events.clear();
	}

	@Test
	void topLevelTestClassesWithSharedApplicationContext() {
		runTestClasses(5, TestCase1.class, TestCase2.class, TestCase3.class, TestCase4.class, TestCase5.class);

		assertThat(EventTracker.events).containsExactly(

			// --- TestCase1 -----------------------------------------------
			// Refreshed instead of Restarted, since this is the first time
			// the context is loaded.
			"ContextRefreshed:TestCase1",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase1",
			"ContextPaused:TestCase1",

			// --- TestCase2 -----------------------------------------------
			"ContextRestarted:TestCase1",
			"BeforeTestClass:TestCase2",
			"AfterTestClass:TestCase2",
			"ContextPaused:TestCase1",

			// --- TestCase3 -----------------------------------------------
			"ContextRestarted:TestCase1",
			"BeforeTestClass:TestCase3",
			"AfterTestClass:TestCase3",
			// Closed instead of Stopped, since TestCase3 uses @DirtiesContext
			"ContextClosed:TestCase1",

			// --- TestCase4 -----------------------------------------------
			// Refreshed instead of Restarted, since TestCase3 uses @DirtiesContext
			"ContextRefreshed:TestCase4",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.
			"AfterTestClass:TestCase4",
			"ContextPaused:TestCase4",

			// --- TestCase5 -----------------------------------------------
			"ContextRestarted:TestCase4",
			"BeforeTestClass:TestCase5",
			"AfterTestClass:TestCase5",
			"ContextPaused:TestCase4"
		);
	}

	@Test
	void testClassesInNestedTestHierarchy() {
		runTestClasses(5, EnclosingTestCase.class);

		assertThat(EventTracker.events).containsExactly(

			// --- EnclosingTestCase -------------------------------------------
			"ContextRefreshed:EnclosingTestCase",
			// No BeforeTestClass, since EventPublishingTestExecutionListener
			// only publishes events for a context that has already been loaded.

				// --- NestedTestCase ------------------------------------------
				// No Refreshed or Restarted event, since NestedTestCase shares the
				// active context used by EnclosingTestCase.
				"BeforeTestClass:NestedTestCase",

					// --- OverridingNestedTestCase1 ---------------------------
					"ContextRefreshed:OverridingNestedTestCase1",
					// No BeforeTestClass, since EventPublishingTestExecutionListener
					// only publishes events for a context that has already been loaded.

						// --- InheritingNestedTestCase ------------------------
						// No Refreshed or Restarted event, since InheritingNestedTestCase
						// shares the active context used by OverridingNestedTestCase1.
						"BeforeTestClass:InheritingNestedTestCase",
						"AfterTestClass:InheritingNestedTestCase",
						// No Stopped event, since OverridingNestedTestCase1 is still
						// using the context

					"AfterTestClass:OverridingNestedTestCase1",
					"ContextPaused:OverridingNestedTestCase1",

					// --- OverridingNestedTestCase2 ---------------------------
					"ContextRestarted:OverridingNestedTestCase1",
					"BeforeTestClass:OverridingNestedTestCase2",
					"AfterTestClass:OverridingNestedTestCase2",
					"ContextPaused:OverridingNestedTestCase1",

				"AfterTestClass:NestedTestCase",
				// No Stopped event, since EnclosingTestCase is still using the context

			"AfterTestClass:EnclosingTestCase",
			"ContextPaused:EnclosingTestCase"
		);
	}

	@Test
	void testClassesWithContextHierarchies() {
		runTestClasses(5,
			ContextHierarchyLevel1TestCase.class,
			ContextHierarchyLevel2TestCase.class,
			ContextHierarchyLevel3a1TestCase.class,
			ContextHierarchyLevel3a2TestCase.class,
			ContextHierarchyLevel3bTestCase.class
		);

		assertThat(EventTracker.events).containsExactly(

			// --- ContextHierarchyLevel1TestCase ------------------------------
			"ContextRefreshed:ContextHierarchyLevel1TestCase",
			"AfterTestClass:ContextHierarchyLevel1TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase",

			// --- ContextHierarchyLevel2TestCase ------------------------------
			"ContextRestarted:ContextHierarchyLevel1TestCase",
			"ContextRefreshed:ContextHierarchyLevel2TestCase",
			"AfterTestClass:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase",

			// --- ContextHierarchyLevel3a1TestCase -----------------------------
			"ContextRestarted:ContextHierarchyLevel1TestCase",
			"ContextRestarted:ContextHierarchyLevel2TestCase",
			"ContextRefreshed:ContextHierarchyLevel3a1TestCase",
			"AfterTestClass:ContextHierarchyLevel3a1TestCase",
			"ContextPaused:ContextHierarchyLevel3a1TestCase",
			"ContextPaused:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase",

			// --- ContextHierarchyLevel3a2TestCase -----------------------------
			"ContextRestarted:ContextHierarchyLevel1TestCase",
			"ContextRestarted:ContextHierarchyLevel2TestCase",
			"ContextRestarted:ContextHierarchyLevel3a1TestCase",
			"BeforeTestClass:ContextHierarchyLevel3a2TestCase",
			"AfterTestClass:ContextHierarchyLevel3a2TestCase",
			"ContextPaused:ContextHierarchyLevel3a1TestCase",
			"ContextPaused:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase",

			// --- ContextHierarchyLevel3bTestCase -----------------------------
			"ContextRestarted:ContextHierarchyLevel1TestCase",
			"ContextRestarted:ContextHierarchyLevel2TestCase",
			"ContextRefreshed:ContextHierarchyLevel3bTestCase",
			"AfterTestClass:ContextHierarchyLevel3bTestCase",
			"ContextPaused:ContextHierarchyLevel3bTestCase",
			"ContextPaused:ContextHierarchyLevel2TestCase",
			"ContextPaused:ContextHierarchyLevel1TestCase"
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

	@SpringJUnitConfig(EventTracker.class)
	@ContextCustomizerFactories(DisplayNameCustomizerFactory.class)
	@TestPropertySource(properties = "magicKey = puzzle")
	static class EnclosingTestCase {

		@Test
		void test(@Value("${magicKey}") String magicKey) {
			assertThat(magicKey).isEqualTo("puzzle");
		}

		@Nested
		@TestClassOrder(ClassOrderer.OrderAnnotation.class)
		class NestedTestCase {

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
	}

}
