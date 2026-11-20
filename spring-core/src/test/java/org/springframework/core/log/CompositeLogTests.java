/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.log;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
