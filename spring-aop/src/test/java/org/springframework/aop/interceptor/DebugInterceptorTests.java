/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DebugInterceptor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class DebugInterceptorTests {

	@Test
	void sunnyDayPathLogsCorrectly() throws Throwable {
		MethodInvocation methodInvocation = mock();

		Log log = mock();
		given(log.isTraceEnabled()).willReturn(true);

		DebugInterceptor interceptor = new StubDebugInterceptor(log);
		interceptor.invoke(methodInvocation);
		checkCallCountTotal(interceptor);

		verify(log, times(2)).trace(anyString());
	}

	@Test
	void exceptionPathStillLogsCorrectly() throws Throwable {
		MethodInvocation methodInvocation = mock();

		IllegalArgumentException exception = new IllegalArgumentException();
		given(methodInvocation.proceed()).willThrow(exception);

		Log log = mock();
		given(log.isTraceEnabled()).willReturn(true);

		DebugInterceptor interceptor = new StubDebugInterceptor(log);
		assertThatIllegalArgumentException().isThrownBy(() ->
				interceptor.invoke(methodInvocation));
		checkCallCountTotal(interceptor);

		verify(log).trace(anyString());
		verify(log).trace(anyString(), eq(exception));
	}

	private void checkCallCountTotal(DebugInterceptor interceptor) {
		assertThat(interceptor.getCount()).as("Intercepted call count not being incremented correctly").isEqualTo(1);
	}


	@SuppressWarnings("serial")
	private static final class StubDebugInterceptor extends DebugInterceptor {

		private final Log log;


		public StubDebugInterceptor(Log log) {
			super(true);
			this.log = log;
		}

		@Override
		protected Log getLoggerForInvocation(MethodInvocation invocation) {
			return log;
		}

	}

}
