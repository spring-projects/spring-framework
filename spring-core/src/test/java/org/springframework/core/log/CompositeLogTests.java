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

package org.springframework.core.log;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;


/**
 * Tests for {@link CompositeLog}.
 *
 * @author Rossen Stoyanchev
 */
class CompositeLogTests {

	private final Log logger1 = mock();

	private final Log logger2 = mock();

	private final CompositeLog compositeLog = new CompositeLog(Arrays.asList(logger1, logger2));


	@Test
	void useFirstLogger() {
		when(logger1.isInfoEnabled()).thenReturn(true);
		when(logger2.isInfoEnabled()).thenReturn(true);

		this.compositeLog.info("info message");

		verify(this.logger1).isInfoEnabled();
		verify(this.logger1).info("info message");

		verifyNoMoreInteractions(this.logger1);
		verifyNoMoreInteractions(this.logger2);
	}

	@Test
	void useSecondLogger() {
		when(logger1.isInfoEnabled()).thenReturn(false);
		when(logger2.isInfoEnabled()).thenReturn(true);

		this.compositeLog.info("info message");

		verify(this.logger1).isInfoEnabled();
		verify(this.logger2).isInfoEnabled();
		verify(this.logger2).info("info message");

		verifyNoMoreInteractions(this.logger1);
		verifyNoMoreInteractions(this.logger2);
	}

	@Test
	void useNeitherLogger() {
		when(logger1.isInfoEnabled()).thenReturn(false);
		when(logger2.isInfoEnabled()).thenReturn(false);

		this.compositeLog.info("info message");

		verify(this.logger1).isInfoEnabled();
		verify(this.logger2).isInfoEnabled();

		verifyNoMoreInteractions(this.logger1);
		verifyNoMoreInteractions(this.logger2);
	}

}
