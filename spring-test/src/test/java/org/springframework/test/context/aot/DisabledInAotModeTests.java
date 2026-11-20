/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.aot.AotDetector;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.container;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.skippedWithReason;

/**
 * Tests for {@link DisabledInAotMode @DisabledInAotMode}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class DisabledInAotModeTests {

	@Test
	void defaultDisabledReason() {
		runTestsInAotMode(DefaultReasonTestCase.class, "Disabled in Spring AOT mode");
	}

	@Test
	void customDisabledReason() {
		runTestsInAotMode(CustomReasonTestCase.class, "Disabled in Spring AOT mode ==> @ContextHierarchy is not supported in AOT");
	}


	private static void runTestsInAotMode(Class<?> testClass, String expectedReason) {
		try {
			System.setProperty(AotDetector.AOT_ENABLED, "true");

			EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(testClass))
				.execute()
				.allEvents()
				.assertThatEvents().haveExactly(1,
					event(container(testClass.getSimpleName()), skippedWithReason(expectedReason)));
		}
		finally {
			System.clearProperty(AotDetector.AOT_ENABLED);
		}
	}


	@DisabledInAotMode
	static class DefaultReasonTestCase {

		@Test
		void test() {
		}
	}

	@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
	static class CustomReasonTestCase {

		@Test
		void test() {
		}
	}

}
