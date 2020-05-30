/*
 * Copyright 2002-2019 the original author or authors.
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

import com.jamonapi.MonitorFactory;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Steve Souza
 * @since 4.1
 */
public class JamonPerformanceMonitorInterceptorTests {

	private final JamonPerformanceMonitorInterceptor interceptor = new JamonPerformanceMonitorInterceptor();

	private final MethodInvocation mi = mock(MethodInvocation.class);

	private final Log log = mock(Log.class);


	@BeforeEach
	public void setUp() {
		MonitorFactory.reset();
	}

	@AfterEach
	public void tearDown() {
		MonitorFactory.reset();
	}


	@Test
	public void testInvokeUnderTraceWithNormalProcessing() throws Throwable {
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));

		interceptor.invokeUnderTrace(mi, log);

		assertThat(MonitorFactory.getNumRows() > 0).as("jamon must track the method being invoked").isTrue();
		assertThat(MonitorFactory.getReport().contains("toString")).as("The jamon report must contain the toString method that was invoked").isTrue();
	}

	@Test
	public void testInvokeUnderTraceWithExceptionTracking() throws Throwable {
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
		given(mi.proceed()).willThrow(new IllegalArgumentException());

		assertThatIllegalArgumentException().isThrownBy(() ->
				interceptor.invokeUnderTrace(mi, log));

		assertThat(MonitorFactory.getNumRows()).as("Monitors must exist for the method invocation and 2 exceptions").isEqualTo(3);
		assertThat(MonitorFactory.getReport().contains("toString")).as("The jamon report must contain the toString method that was invoked").isTrue();
		assertThat(MonitorFactory.getReport().contains(MonitorFactory.EXCEPTIONS_LABEL)).as("The jamon report must contain the generic exception: " + MonitorFactory.EXCEPTIONS_LABEL).isTrue();
		assertThat(MonitorFactory.getReport().contains("IllegalArgumentException")).as("The jamon report must contain the specific exception: IllegalArgumentException'").isTrue();
	}

}
