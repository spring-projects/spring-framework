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

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.Test;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
public final class PerformanceMonitorInterceptorTests {

	@Test
	public void testSuffixAndPrefixAssignment() {
		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor();

		assertNotNull(interceptor.getPrefix());
		assertNotNull(interceptor.getSuffix());

		interceptor.setPrefix(null);
		interceptor.setSuffix(null);

		assertNotNull(interceptor.getPrefix());
		assertNotNull(interceptor.getSuffix());
	}

	@Test
	public void testSunnyDayPathLogsPerformanceMetricsCorrectly() throws Throwable {
		Log log = createMock(Log.class);
		MethodInvocation mi = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[0]);

		expect(mi.getMethod()).andReturn(toString);
		expect(mi.proceed()).andReturn(null);
		log.trace(isA(String.class));

		replay(mi, log);

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		interceptor.invokeUnderTrace(mi, log);

		verify(mi, log);
	}

	@Test
	public void testExceptionPathStillLogsPerformanceMetricsCorrectly() throws Throwable {
		Log log = createMock(Log.class);
		MethodInvocation mi = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[0]);

		expect(mi.getMethod()).andReturn(toString);
		expect(mi.proceed()).andThrow(new IllegalArgumentException());
		log.trace(isA(String.class));

		replay(mi, log);

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		try {
			interceptor.invokeUnderTrace(mi, log);
			fail("Must have propagated the IllegalArgumentException.");
		}
		catch (IllegalArgumentException expected) {
		}

		verify(mi, log);
	}

}
