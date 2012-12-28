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
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public final class CustomizableTraceInterceptorTests {

	@Test(expected=IllegalArgumentException.class)
	public void testSetEmptyEnterMessage() {
		// Must not be able to set empty enter message
		new CustomizableTraceInterceptor().setEnterMessage("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetEnterMessageWithReturnValuePlaceholder() {
		// Must not be able to set enter message with return value placeholder
		new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetEnterMessageWithExceptionPlaceholder() {
		// Must not be able to set enter message with exception placeholder
		new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetEnterMessageWithInvocationTimePlaceholder() {
		// Must not be able to set enter message with invocation time placeholder
		new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetEmptyExitMessage() {
		// Must not be able to set empty exit message
		new CustomizableTraceInterceptor().setExitMessage("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetExitMessageWithExceptionPlaceholder() {
		// Must not be able to set exit message with exception placeholder
		new CustomizableTraceInterceptor().setExitMessage(CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetEmptyExceptionMessage() {
		// Must not be able to set empty exception message
		new CustomizableTraceInterceptor().setExceptionMessage("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSetExceptionMethodWithReturnValuePlaceholder() {
		// Must not be able to set exception message with return value placeholder
		new CustomizableTraceInterceptor().setExceptionMessage(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE);
	}

	@Test
	public void testSunnyDayPathLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);

		MethodInvocation methodInvocation = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[]{});

		expect(log.isTraceEnabled()).andReturn(true);
		expect(methodInvocation.getMethod()).andReturn(toString).times(4);
		expect(methodInvocation.getThis()).andReturn(this).times(2);
		log.trace(isA(String.class));
		expect(methodInvocation.proceed()).andReturn(null);
		log.trace(isA(String.class));

		replay(methodInvocation);
		replay(log);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.invoke(methodInvocation);

		verify(log);
		verify(methodInvocation);
	}

	@Test
	public void testExceptionPathLogsCorrectly() throws Throwable {
		Log log = createMock(Log.class);

		MethodInvocation methodInvocation = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[]{});

		expect(log.isTraceEnabled()).andReturn(true);
		expect(methodInvocation.getMethod()).andReturn(toString).times(4);
		expect(methodInvocation.getThis()).andReturn(this).times(2);
		log.trace(isA(String.class));
		IllegalArgumentException exception = new IllegalArgumentException();
		expect(methodInvocation.proceed()).andThrow(exception);
		log.trace(isA(String.class), eq(exception));

		replay(log);
		replay(methodInvocation);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		try {
			interceptor.invoke(methodInvocation);
			fail("Must have propagated the IllegalArgumentException.");
		}
		catch (IllegalArgumentException expected) {
		}

		verify(log);
		verify(methodInvocation);
	}

	@Test
	public void testSunnyDayPathLogsCorrectlyWithPrettyMuchAllPlaceholdersMatching() throws Throwable {
		Log log = createMock(Log.class);

		MethodInvocation methodInvocation = createMock(MethodInvocation.class);

		Method toString = String.class.getMethod("toString", new Class[0]);
		Object[] arguments = new Object[]{"$ One \\$", new Long(2)};

		expect(log.isTraceEnabled()).andReturn(true);
		expect(methodInvocation.getMethod()).andReturn(toString).times(7);
		expect(methodInvocation.getThis()).andReturn(this).times(2);
		expect(methodInvocation.getArguments()).andReturn(arguments).times(2);
		log.trace(isA(String.class));
		expect(methodInvocation.proceed()).andReturn("Hello!");
		log.trace(isA(String.class));

		replay(methodInvocation);
		replay(log);

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.setEnterMessage(new StringBuffer()
			.append("Entering the '").append(CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME)
			.append("' method of the [").append(CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_NAME)
			.append("] class with the following args (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS)
			.append(") and arg types (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES)
			.append(").").toString());
		interceptor.setExitMessage(new StringBuffer()
			.append("Exiting the '").append(CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME)
			.append("' method of the [").append(CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_SHORT_NAME)
			.append("] class with the following args (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS)
			.append(") and arg types (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES)
			.append("), returning '").append(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE)
			.append("' and taking '").append(CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME)
			.append("' this long.").toString());
		interceptor.invoke(methodInvocation);

		verify(log);
		verify(methodInvocation);
	}


	@SuppressWarnings("serial")
	private static class StubCustomizableTraceInterceptor extends CustomizableTraceInterceptor {

		private final Log log;

		public StubCustomizableTraceInterceptor(Log log) {
			super.setUseDynamicLogger(false);
			this.log = log;
		}

		@Override
		protected Log getLoggerForInvocation(MethodInvocation invocation) {
			return this.log;
		}
	}

}
