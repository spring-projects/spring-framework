/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 14.03.2003
 */
class MethodInvocationTests {

	@Test
	void validInvocation() throws Throwable {
		Method method = Object.class.getMethod("hashCode");
		Object proxy = new Object();
		Object returnValue = new Object();
		List<Object> interceptors = Collections.singletonList((MethodInterceptor) invocation -> returnValue);
		ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(proxy, null, method, null, null, interceptors);
		Object rv = invocation.proceed();
		assertThat(rv).as("correct response").isSameAs(returnValue);
	}

	/**
	 * toString on target can cause failure.
	 */
	@Test
	void toStringDoesntHitTarget() throws Throwable {
		Object target = new TestBean() {
			@Override
			public String toString() {
				throw new UnsupportedOperationException("toString");
			}
		};
		List<Object> interceptors = Collections.emptyList();

		Method m = Object.class.getMethod("hashCode");
		Object proxy = new Object();
		ReflectiveMethodInvocation invocation = new ReflectiveMethodInvocation(proxy, target, m, null, null, interceptors);

		// If it hits target, the test will fail with the UnsupportedOpException
		// in the inner class above.
		invocation.toString();
	}

}
