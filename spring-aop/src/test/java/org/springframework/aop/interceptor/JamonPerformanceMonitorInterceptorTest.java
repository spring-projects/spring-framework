
/*
 * Copyright 2002-2014 the original author or authors.
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

import com.jamonapi.MonitorFactory;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

public class JamonPerformanceMonitorInterceptorTest {
    private JamonPerformanceMonitorInterceptor interceptor = new JamonPerformanceMonitorInterceptor();
    private MethodInvocation mi = mock(MethodInvocation.class);
    private Log log = mock(Log.class);

    @Before
    public void setUp() {
       MonitorFactory.reset();
    }

    @After
    public void tearDown() {
        MonitorFactory.reset();
    }

    @Test
    public void testInvokeUnderTraceWithNormalProcessing() throws Throwable {
        given(mi.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));

        interceptor.invokeUnderTrace(mi, log);

        assertEquals("jamon must track the method being invoked", 1, MonitorFactory.getNumRows());
        assertTrue("The jamon report must contain the toString method that was invoked", MonitorFactory.getReport().contains("toString"));
    }


    @Test
    public void testInvokeUnderTraceWithException() throws Throwable {
        given(mi.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));
        given(mi.proceed()).willThrow(new IllegalArgumentException());

        try {
            interceptor.invokeUnderTrace(mi, log);
            fail("Must have propagated the IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
        }

        assertEquals("A monitor must exist for the method being invoked", 1, MonitorFactory.getNumRows());
        assertTrue("The jamon report must contain the toString method that was invoked",  MonitorFactory.getReport().contains("toString"));
    }

    @Test
    public void testInvokeUnderTraceWithExceptionTracking() throws Throwable {
        interceptor.setTrackExceptions(true);
        given(mi.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));
        given(mi.proceed()).willThrow(new IllegalArgumentException());

        try {
            interceptor.invokeUnderTrace(mi, log);
            fail("Must have propagated the IllegalArgumentException.");
        } catch (IllegalArgumentException expected) {
        }

        assertEquals("Monitors must exist for the method invocation and 2 exceptions", 3, MonitorFactory.getNumRows());
        assertTrue("The jamon report must contain the toString method that was invoked", MonitorFactory.getReport().contains("toString"));
        assertTrue("The jamon report must contain the generic exception: "+MonitorFactory.EXCEPTIONS_LABEL,  MonitorFactory.getReport().contains(MonitorFactory.EXCEPTIONS_LABEL));
        assertTrue("The jamon report must contain the specific exception: IllegalArgumentException'",  MonitorFactory.getReport().contains("IllegalArgumentException"));
    }
}