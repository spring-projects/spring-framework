/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MockitoResetTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see MockitoResetTestExecutionListenerWithContextLoadFailureTests
 * @see MockitoResetTestExecutionListenerWithContextLoadFailureTestNGTests
 */
class MockitoResetTestExecutionListenerTests {

	private final TestContext testContext = mock();


	@Test
	void beforeTestMethodIsNoOpWhenContextIsNotAvailable() {
		when(testContext.hasApplicationContext()).thenReturn(false);

		new MockitoResetTestExecutionListener().beforeTestMethod(testContext);

		verify(testContext).hasApplicationContext();
		verify(testContext, never()).getApplicationContext();
	}

	@Test
	void afterTestMethodIsNoOpWhenContextIsNotAvailable() {
		when(testContext.hasApplicationContext()).thenReturn(false);

		new MockitoResetTestExecutionListener().afterTestMethod(testContext);

		verify(testContext).hasApplicationContext();
		verify(testContext, never()).getApplicationContext();
	}

}
