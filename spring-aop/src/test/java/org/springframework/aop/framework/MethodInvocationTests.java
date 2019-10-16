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

package org.springframework.aop.framework;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 14.03.2003
 */
class MethodInvocationTests {

	@Test
	void testValidInvocation() throws Throwable {
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
	void testToStringDoesntHitTarget() throws Throwable {
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
