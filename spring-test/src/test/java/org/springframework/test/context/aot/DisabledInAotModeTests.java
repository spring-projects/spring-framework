/*
 * Copyright 2002-2024 the original author or authors.
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
