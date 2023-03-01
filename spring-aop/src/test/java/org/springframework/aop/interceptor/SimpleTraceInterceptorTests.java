/*
 * Copyright 2002-2023 the original author or authors.
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
 * Unit tests for the {@link SimpleTraceInterceptor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class SimpleTraceInterceptorTests {

	@Test
	public void testSunnyDayPathLogsCorrectly() throws Throwable {
		MethodInvocation mi = mock();
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
		given(mi.getThis()).willReturn(this);

		Log log = mock();

		SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
		interceptor.invokeUnderTrace(mi, log);

		verify(log, times(2)).trace(anyString());
	}

	@Test
	public void testExceptionPathStillLogsCorrectly() throws Throwable {
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
