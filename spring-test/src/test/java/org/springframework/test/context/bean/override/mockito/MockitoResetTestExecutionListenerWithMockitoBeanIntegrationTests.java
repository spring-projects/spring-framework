/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoResetTestExecutionListener} with a
 * {@link MockitoBean @MockitoBean} field.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoResetTestExecutionListenerWithoutMockitoAnnotationsIntegrationTests
 * @see MockResetStrategiesIntegrationTests
 */
class MockitoResetTestExecutionListenerWithMockitoBeanIntegrationTests
		extends MockitoResetTestExecutionListenerWithoutMockitoAnnotationsIntegrationTests {

	// We declare the following to ensure that MockReset is also supported with
	// @MockitoBean or @MockitoSpyBean fields present in the test class.
	@MockitoBean
	PuzzleService puzzleService;


	// test001() and test002() are in the superclass.

	@Test
	void test003() {
		given(puzzleService.getAnswer()).willReturn("enigma");
		assertThat(puzzleService.getAnswer()).isEqualTo("enigma");
	}


	interface PuzzleService {

		String getAnswer();
	}

}
