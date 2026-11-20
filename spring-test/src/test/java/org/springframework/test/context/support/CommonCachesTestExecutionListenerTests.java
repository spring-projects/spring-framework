/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.TestContext;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link CommonCachesTestExecutionListener}.
 *
 * @author Stephane Nicoll
 */
class CommonCachesTestExecutionListenerTests {

	private final CommonCachesTestExecutionListener listener = new CommonCachesTestExecutionListener();

	@Test
	void afterTestClassWhenContextIsAvailable() throws Exception {
		AbstractApplicationContext applicationContext = mock();
		TestContext testContext = mock(TestContext.class);
		given(testContext.hasApplicationContext()).willReturn(true);
		given(testContext.getApplicationContext()).willReturn(applicationContext);
		listener.afterTestClass(testContext);
		verify(applicationContext).clearResourceCaches();
	}

	@Test
	void afterTestClassCWhenContextIsNotAvailable() throws Exception {
		TestContext testContext = mock();
		given(testContext.hasApplicationContext()).willReturn(false);
		listener.afterTestClass(testContext);
		verify(testContext).hasApplicationContext();
		verifyNoMoreInteractions(testContext);
	}

}
