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

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.easymock.MockControl;

import org.springframework.test.AssertThrows;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class CustomizableTraceInterceptorTests extends TestCase {

	public void testSetEmptyEnterMessage() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set empty enter message") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setEnterMessage("");
			}
		}.runTest();
	}

	public void testSetEnterMessageWithReturnValuePlaceholder() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set enter message with return value placeholder") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE);
			}
		}.runTest();
	}

	public void testSetEnterMessageWithExceptionPlaceholder() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set enter message with exception placeholder") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION);
			}
		}.runTest();
	}

	public void testSetEnterMessageWithInvocationTimePlaceholder() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set enter message with invocation time placeholder") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setEnterMessage(CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME);
			}
		}.runTest();
	}

	public void testSetEmptyExitMessage() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set empty exit message") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setExitMessage("");
			}
		}.runTest();
	}

	public void testSetExitMessageWithExceptionPlaceholder() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set exit message with exception placeholder") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setExitMessage(CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION);
			}
		}.runTest();
	}

	public void testSetEmptyExceptionMessage() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set empty exception message") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setExceptionMessage("");
			}
		}.runTest();
	}

	public void testSetExceptionMethodWithReturnValuePlaceholder() {
		new AssertThrows(IllegalArgumentException.class, "Must not be able to set exception message with return value placeholder") {
			public void test() throws Exception {
				new CustomizableTraceInterceptor().setExceptionMessage(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE);
			}
		}.runTest();
	}

	public void testSunnyDayPathLogsCorrectly() throws Throwable {
		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();

		MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

		Method toString = String.class.getMethod("toString", new Class[]{});

		log.isTraceEnabled();
		mockLog.setReturnValue(true);
		methodInvocation.getMethod();
		mockMethodInvocation.setReturnValue(toString, 4);
		methodInvocation.getThis();
		mockMethodInvocation.setReturnValue(this, 2);
		log.trace("Some tracing output");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		methodInvocation.proceed();
		mockMethodInvocation.setReturnValue(null);
		log.trace("Some more tracing output");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		mockLog.setVoidCallable();

		mockMethodInvocation.replay();
		mockLog.replay();

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.invoke(methodInvocation);

		mockLog.verify();
		mockMethodInvocation.verify();
	}

	public void testExceptionPathLogsCorrectly() throws Throwable {
		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();

		MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

		Method toString = String.class.getMethod("toString", new Class[]{});

		log.isTraceEnabled();
		mockLog.setReturnValue(true);
		methodInvocation.getMethod();
		mockMethodInvocation.setReturnValue(toString, 4);
		methodInvocation.getThis();
		mockMethodInvocation.setReturnValue(this, 2);
		log.trace("Some tracing output");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		methodInvocation.proceed();
		IllegalArgumentException exception = new IllegalArgumentException();
		mockMethodInvocation.setThrowable(exception);
		log.trace("Some more tracing output", exception);
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		mockLog.setVoidCallable();

		mockMethodInvocation.replay();
		mockLog.replay();

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		try {
			interceptor.invoke(methodInvocation);
			fail("Must have propagated the IllegalArgumentException.");
		}
		catch (IllegalArgumentException expected) {
		}

		mockLog.verify();
		mockMethodInvocation.verify();
	}

	public void testSunnyDayPathLogsCorrectlyWithPrettyMuchAllPlaceholdersMatching() throws Throwable {
		MockControl mockLog = MockControl.createControl(Log.class);
		Log log = (Log) mockLog.getMock();

		MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
		MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

		Method toString = String.class.getMethod("toString", new Class[0]);
		Object[] arguments = new Object[]{"$ One \\$", new Long(2)};

		log.isTraceEnabled();
		mockLog.setReturnValue(true);
		methodInvocation.getMethod();
		mockMethodInvocation.setReturnValue(toString, 7);
		methodInvocation.getThis();
		mockMethodInvocation.setReturnValue(this, 2);
		methodInvocation.getArguments();
		mockMethodInvocation.setReturnValue(arguments, 2);
		log.trace("Some tracing output");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		methodInvocation.proceed();
		mockMethodInvocation.setReturnValue("Hello!");
		log.trace("Some more tracing output");
		mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
		mockLog.setVoidCallable();

		mockMethodInvocation.replay();
		mockLog.replay();

		CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
		interceptor.setEnterMessage(new StringBuffer().append("Entering the '").append(CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME).append("' method of the [").append(CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_NAME).append("] class with the following args (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS).append(") and arg types (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES).append(").").toString());
		interceptor.setExitMessage(new StringBuffer().append("Exiting the '").append(CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME).append("' method of the [").append(CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_SHORT_NAME).append("] class with the following args (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS).append(") and arg types (").append(CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES).append("), returning '").append(CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE).append("' and taking '").append(CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME).append("' this long.").toString());
		interceptor.invoke(methodInvocation);

		mockLog.verify();
		mockMethodInvocation.verify();
	}


	private static class StubCustomizableTraceInterceptor extends CustomizableTraceInterceptor {

		private final Log log;

		public StubCustomizableTraceInterceptor(Log log) {
			super.setUseDynamicLogger(false);
			this.log = log;
		}

		protected Log getLoggerForInvocation(MethodInvocation invocation) {
			return this.log;
		}
	}

}
