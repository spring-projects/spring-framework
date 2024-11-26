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

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.test.context.bean.override.mockito.MockResetStrategiesIntegrationTests.MockVerificationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} fields with different
 * {@link MockReset} strategies.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see MockitoResetTestExecutionListenerWithoutMockitoAnnotationsIntegrationTests
 * @see MockitoResetTestExecutionListenerWithMockitoBeanIntegrationTests
 */
// The MockVerificationExtension MUST be registered before the SpringExtension.
@ExtendWith(MockVerificationExtension.class)
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class MockResetStrategiesIntegrationTests {

	static PuzzleService puzzleServiceNoneStaticReference;
	static PuzzleService puzzleServiceBeforeStaticReference;
	static PuzzleService puzzleServiceAfterStaticReference;


	@MockitoBean(name = "puzzleServiceNone", reset = MockReset.NONE)
	PuzzleService puzzleServiceNone;

	@MockitoBean(name = "puzzleServiceBefore", reset = MockReset.BEFORE)
	PuzzleService puzzleServiceBefore;

	@MockitoBean(name = "puzzleServiceAfter", reset = MockReset.AFTER)
	PuzzleService puzzleServiceAfter;


	@AfterEach
	void trackStaticReferences() {
		puzzleServiceNoneStaticReference = this.puzzleServiceNone;
		puzzleServiceBeforeStaticReference = this.puzzleServiceBefore;
		puzzleServiceAfterStaticReference = this.puzzleServiceAfter;
	}

	@AfterAll
	static void releaseStaticReferences() {
		puzzleServiceNoneStaticReference = null;
		puzzleServiceBeforeStaticReference = null;
		puzzleServiceAfterStaticReference = null;
	}


	@Test
	void test001(TestInfo testInfo) {
		assertThat(puzzleServiceNone.getAnswer()).isNull();
		assertThat(puzzleServiceBefore.getAnswer()).isNull();
		assertThat(puzzleServiceAfter.getAnswer()).isNull();

		stubAndTestMocks(testInfo);
	}

	@Test
	void test002(TestInfo testInfo) {
		// Should not have been reset.
		assertThat(puzzleServiceNone.getAnswer()).isEqualTo("none - test001");

		// Should have been reset.
		assertThat(puzzleServiceBefore.getAnswer()).isNull();
		assertThat(puzzleServiceAfter.getAnswer()).isNull();

		stubAndTestMocks(testInfo);
	}

	private void stubAndTestMocks(TestInfo testInfo) {
		String name = testInfo.getTestMethod().get().getName();
		given(puzzleServiceNone.getAnswer()).willReturn("none - " + name);
		assertThat(puzzleServiceNone.getAnswer()).isEqualTo("none - " + name);

		given(puzzleServiceBefore.getAnswer()).willReturn("before - " + name);
		assertThat(puzzleServiceBefore.getAnswer()).isEqualTo("before - " + name);

		given(puzzleServiceAfter.getAnswer()).willReturn("after - " + name);
		assertThat(puzzleServiceAfter.getAnswer()).isEqualTo("after - " + name);
	}

	interface PuzzleService {

		String getAnswer();
	}

	static class MockVerificationExtension implements AfterEachCallback {

		@Override
		public void afterEach(ExtensionContext context) throws Exception {
			String name = context.getRequiredTestMethod().getName();

			// Should not have been reset.
			assertThat(puzzleServiceNoneStaticReference.getAnswer()).as("puzzleServiceNone").isEqualTo("none - " + name);
			assertThat(puzzleServiceBeforeStaticReference.getAnswer()).as("puzzleServiceBefore").isEqualTo("before - " + name);

			// Should have been reset.
			assertThat(puzzleServiceAfterStaticReference.getAnswer()).as("puzzleServiceAfter").isNull();
		}
	}

}
