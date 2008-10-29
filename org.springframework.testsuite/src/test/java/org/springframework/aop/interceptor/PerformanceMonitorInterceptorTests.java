/*
 * Copyright 2002-2007 the original author or authors.
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

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.easymock.MockControl;

/**
 * @author Rob Harrop
 * @author Rick Evans
 */
public class PerformanceMonitorInterceptorTests extends TestCase {

	public void testSuffixAndPrefixAssignment() {
		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor();

		assertNotNull(interceptor.getPrefix());
		assertNotNull(interceptor.getSuffix());

		interceptor.setPrefix(null);
		interceptor.setSuffix(null);

		assertNotNull(interceptor.getPrefix());
		assertNotNull(interceptor.getSuffix());
	}

	public void testSunnyDayPathLogsPerformanceMetricsCorrectly() throws Throwable {
		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();

		MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

		Method toString = String.class.getMethod("toString", new Class[0]);

		methodInvocation.getMethod();
		mockMethodInvocation.setReturnValue(toString);
		methodInvocation.proceed();
		mockMethodInvocation.setReturnValue(null);
		log.trace("Some performance metric");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		mockLog.setVoidCallable();

		mockMethodInvocation.replay();
		mockLog.replay();

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		interceptor.invokeUnderTrace(methodInvocation, log);

		mockLog.verify();
		mockMethodInvocation.verify();
	}

	public void testExceptionPathStillLogsPerformanceMetricsCorrectly() throws Throwable {
		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();

		MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

		Method toString = String.class.getMethod("toString", new Class[0]);

		methodInvocation.getMethod();
		mockMethodInvocation.setReturnValue(toString);
		methodInvocation.proceed();
		mockMethodInvocation.setThrowable(new IllegalArgumentException());
		log.trace("Some performance metric");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		mockLog.setVoidCallable();

		mockMethodInvocation.replay();
		mockLog.replay();

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		try {
			interceptor.invokeUnderTrace(methodInvocation, log);
			fail("Must have propagated the IllegalArgumentException.");
		}
		catch (IllegalArgumentException expected) {
		}

		mockLog.verify();
		mockMethodInvocation.verify();
	}

}
