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
