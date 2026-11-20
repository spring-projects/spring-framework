/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Chris Beams
 */
class PerformanceMonitorInterceptorTests {

	@Test
	void suffixAndPrefixAssignment() {
		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor();

		assertThat(interceptor.getPrefix()).isNotNull();
		assertThat(interceptor.getSuffix()).isNotNull();

		interceptor.setPrefix(null);
		interceptor.setSuffix(null);

		assertThat(interceptor.getPrefix()).isNotNull();
		assertThat(interceptor.getSuffix()).isNotNull();
	}

	@Test
	void sunnyDayPathLogsPerformanceMetricsCorrectly() throws Throwable {
		MethodInvocation mi = mock();
		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));

		Log log = mock();

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		interceptor.invokeUnderTrace(mi, log);

		verify(log).trace(anyString());
	}

	@Test
	void exceptionPathStillLogsPerformanceMetricsCorrectly() throws Throwable {
		MethodInvocation mi = mock();

		given(mi.getMethod()).willReturn(String.class.getMethod("toString"));
		given(mi.proceed()).willThrow(new IllegalArgumentException());
		Log log = mock();

		PerformanceMonitorInterceptor interceptor = new PerformanceMonitorInterceptor(true);
		assertThatIllegalArgumentException().isThrownBy(() ->
				interceptor.invokeUnderTrace(mi, log));

		verify(log).trace(anyString());
	}

}
