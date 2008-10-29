/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.easymock.MockControl;

import java.lang.reflect.Method;

/**
 * Unit tests for the {@link SimpleTraceInterceptor} class.
 *
 * @author Rick Evans
 */
public final class SimpleTraceInterceptorTests extends TestCase {

    public void testSunnyDayPathLogsCorrectly() throws Throwable {
        MockControl mockLog = MockControl.createControl(Log.class);
        Log log = (Log) mockLog.getMock();

        MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
        MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

        Method toString = String.class.getMethod("toString", new Class[]{});

        methodInvocation.getMethod();
        mockMethodInvocation.setReturnValue(toString);
        methodInvocation.getThis();
        mockMethodInvocation.setReturnValue(this);
        log.trace("Some tracing output");
        mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
        methodInvocation.proceed();
        mockMethodInvocation.setReturnValue(null);
        log.trace("Some more tracing output");
        mockLog.setMatcher(MockControl.ALWAYS_MATCHER);
        mockLog.setVoidCallable();

        mockMethodInvocation.replay();
        mockLog.replay();

        SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);
        interceptor.invokeUnderTrace(methodInvocation, log);

        mockLog.verify();
        mockMethodInvocation.verify();
    }

    public void testExceptionPathStillLogsCorrectly() throws Throwable {
        MockControl mockLog = MockControl.createControl(Log.class);
        final Log log = (Log) mockLog.getMock();

        MockControl mockMethodInvocation = MockControl.createControl(MethodInvocation.class);
        final MethodInvocation methodInvocation = (MethodInvocation) mockMethodInvocation.getMock();

        Method toString = String.class.getMethod("toString", new Class[]{});

        methodInvocation.getMethod();
        mockMethodInvocation.setReturnValue(toString);
        methodInvocation.getThis();
        mockMethodInvocation.setReturnValue(this);
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

        final SimpleTraceInterceptor interceptor = new SimpleTraceInterceptor(true);

        try {
            interceptor.invokeUnderTrace(methodInvocation, log);
            fail("Must have propagated the IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
        }

        mockLog.verify();
        mockMethodInvocation.verify();
    }

}
