/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.interceptor;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.Test;

/**
 * Unit tests for the {@link SimpleTraceInterceptor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public final class SimpleTraceInterceptorTests {

	@Test
	public void testSunnyDayPathLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);
		MethodInvocation mi = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[]{});

		expect(mi.getMethod()).andReturn(toString);
		expect(mi.getThis()).andReturn(this);
		log.trace(isA(String.class));
		expect(mi.proceed()).andReturn(null);
		log.trace(isA(String.class));

		replay(mi, log);

		SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
		interceptor.invokeUnderTrace(mi, log);

		verify(mi, log);
	}

	public void testExceptionPathStillLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);
		MethodInvocation mi = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[]{});

		expect(mi.getMethod()).andReturn(toString);
		expect(mi.getThis()).andReturn(this);
		log.trace(isA(String.class));
		IllegalArgumentException exception = new IllegalArgumentException();
		expect(mi.proceed()).andThrow(exception);
		log.trace(isA(String.class));

		replay(mi, log);

		final SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);

		try {
			interceptor.invokeUnderTrace(mi, log);
			fail("Must have propagated the IllegalArgumentException.");
		} catch (IllegalArgumentException expected) {
		}

		verify(mi, log);
	}

}
