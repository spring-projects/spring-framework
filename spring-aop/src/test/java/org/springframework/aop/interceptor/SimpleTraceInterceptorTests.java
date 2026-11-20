/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpleTraceInterceptor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class SimpleTraceInterceptorTests {

	@Test
	void sunnyDayPathLogsCorrectly() throws Throwable {
		MethodInvocation mi = mock();
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
		given(mi.getThis()).willReturn(this);

		Log log = mock();

		SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
		interceptor.invokeUnderTrace(mi, log);

		verify(log, times(2)).trace(anyString());
	}

	@Test
	void exceptionPathStillLogsCorrectly() throws Throwable {
		MethodInvocation mi = mock();
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
		given(mi.getThis()).willReturn(this);
		IllegalArgumentException exception = new IllegalArgumentException();
		given(mi.proceed()).willThrow(exception);

		Log log = mock();

		final SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
		assertThatIllegalArgumentException().isThrownBy(() ->
			interceptor.invokeUnderTrace(mi, log));

		verify(log).trace(anyString());
		verify(log).trace(anyString(), eq(exception));
	}

}
