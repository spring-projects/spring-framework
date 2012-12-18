/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.interceptor;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.Test;

/**
 * Unit tests for the {@link DebugInterceptor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class DebugInterceptorTests {

	@Test
	public void testSunnyDayPathLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);

		MethodInvocation methodInvocation = createMock(MethodInvocation.class);

		expect(log.isTraceEnabled()).andReturn(true);
		log.trace(isA(String.class));
		expect(methodInvocation.proceed()).andReturn(null);
		log.trace(isA(String.class));

		replay(methodInvocation);
		replay(log);

		DebugInterceptor interceptor = new StubDebugInterceptor(log);
		interceptor.invoke(methodInvocation);
		checkCallCountTotal(interceptor);

		verify(methodInvocation);
		verify(log);
	}

	@Test
	public void testExceptionPathStillLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);

		MethodInvocation methodInvocation = createMock(MethodInvocation.class);

		expect(log.isTraceEnabled()).andReturn(true);
		log.trace(isA(String.class));
		IllegalArgumentException exception = new IllegalArgumentException();
		expect(methodInvocation.proceed()).andThrow(exception);
		log.trace(isA(String.class), eq(exception));

		replay(methodInvocation);
		replay(log);

		DebugInterceptor interceptor = new StubDebugInterceptor(log);
		try {
			interceptor.invoke(methodInvocation);
			fail("Must have propagated the IllegalArgumentException.");
		} catch (IllegalArgumentException expected) {
		}
		checkCallCountTotal(interceptor);

		verify(methodInvocation);
		verify(log);
	}

	private void checkCallCountTotal(DebugInterceptor interceptor) {
		assertEquals("Intercepted call count not being incremented correctly", 1, interceptor.getCount());
	}


	@SuppressWarnings("serial")
	private static final class StubDebugInterceptor extends DebugInterceptor {

		private final Log log;


		public StubDebugInterceptor(Log log) {
			super(true);
			this.log = log;
		}

		protected Log getLoggerForInvocation(MethodInvocation invocation) {
			return log;
		}

	}

}
